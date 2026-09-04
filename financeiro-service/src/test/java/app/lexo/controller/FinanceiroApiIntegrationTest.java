package app.lexo.controller;

import app.lexo.client.ReferenciaGateway;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API do financeiro (integracao)")
class FinanceiroApiIntegrationTest {

    private static final String CHAVE_ASSINATURA = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";
    private static final String CHAVE_INTERNA = "chave-de-teste-interna";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private ReferenciaGateway referencias;

    @BeforeEach
    void referenciasSempreValidas() {
        when(referencias.clienteExiste(any(), any())).thenReturn(true);
        when(referencias.processoExiste(any(), any())).thenReturn(true);
    }

    private record Identidade(String userId, String orgId, String role, String name) {
        static Identidade nova() {
            return nova("ADMIN");
        }

        static Identidade nova(String role) {
            return new Identidade(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    role, "Usuario Teste");
        }

        static Identidade mesmaOrg(Identidade outra, String role) {
            return new Identidade(UUID.randomUUID().toString(), outra.orgId(), role, "Usuario Teste");
        }
    }

    private HttpHeaders comoGateway(Identidade id) {
        return assinado(id, CHAVE_ASSINATURA, Instant.now().getEpochSecond());
    }

    private HttpHeaders assinado(Identidade id, String chave, long timestamp) {
        HttpHeaders h = semAssinatura(id);
        String nome = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(
                id.userId(), id.orgId(), id.role(), nome, "", timestamp);
        h.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        h.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(chave, payload));
        return h;
    }

    private HttpHeaders semAssinatura(Identidade id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", URLEncoder.encode(id.name(), StandardCharsets.UTF_8));
        h.add("X-User-Email", "");
        return h;
    }

    private String honorario(String descricao, String valor) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("clientId", UUID.randomUUID().toString());
        corpo.put("description", descricao);
        corpo.put("amount", valor);
        corpo.put("status", "PENDENTE");
        corpo.put("dueDate", Instant.now().plus(30, ChronoUnit.DAYS).toString());
        try {
            return om.writeValueAsString(corpo);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String criar(Identidade id, String descricao, String valor) throws Exception {
        String resposta = mvc.perform(post("/api/financeiro").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(honorario(descricao, valor)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resposta).get("id").asText();
    }

    @Test
    @DisplayName("sem identidade, o acesso e bloqueado")
    void semIdentidadeBloqueia() throws Exception {
        mvc.perform(get("/api/financeiro"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("headers de identidade sem assinatura do gateway sao recusados")
    void semAssinaturaBloqueia() throws Exception {
        mvc.perform(get("/api/financeiro").headers(semAssinatura(Identidade.nova())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura feita com outra chave e recusada")
    void assinaturaDeOutraChaveBloqueia() throws Exception {
        HttpHeaders forjados = assinado(Identidade.nova(),
                "chave-de-atacante-com-mais-de-32-bytes-aqui", Instant.now().getEpochSecond());

        mvc.perform(get("/api/financeiro").headers(forjados))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura fora da janela de tempo e recusada")
    void assinaturaExpiradaBloqueia() throws Exception {
        HttpHeaders antigos = assinado(Identidade.nova(), CHAVE_ASSINATURA,
                Instant.now().getEpochSecond() - 600);

        mvc.perform(get("/api/financeiro").headers(antigos))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("trocar a organizacao depois de assinar invalida a identidade")
    void organizacaoAdulteradaBloqueia() throws Exception {
        HttpHeaders h = comoGateway(Identidade.nova());
        h.set("X-Org-Id", UUID.randomUUID().toString());

        mvc.perform(get("/api/financeiro").headers(h))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("com identidade, cria e lista honorario")
    void criaEListaHonorario() throws Exception {
        Identidade id = Identidade.nova();

        mvc.perform(post("/api/financeiro").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(honorario("Honorarios contratuais", "1500.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Honorarios contratuais"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        mvc.perform(get("/api/financeiro").headers(comoGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("valor zero ou negativo retorna 400")
    void valorInvalidoRetorna400() throws Exception {
        Identidade id = Identidade.nova();

        mvc.perform(post("/api/financeiro").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(honorario("Honorario sem valor", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Valor deve ser maior que zero"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("descricao vazia retorna 400")
    void descricaoVaziaRetorna400() throws Exception {
        Identidade id = Identidade.nova();

        mvc.perform(post("/api/financeiro").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(honorario("", "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("cliente inexistente no cliente-service retorna 404")
    void clienteInexistenteRetorna404() throws Exception {
        when(referencias.clienteExiste(any(), any())).thenReturn(false);

        mvc.perform(post("/api/financeiro").headers(comoGateway(Identidade.nova()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(honorario("Honorario orfao", "100.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("multi-tenant: uma organizacao nao ve honorarios de outra")
    void isolamentoMultiTenant() throws Exception {
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();

        criar(a, "Honorario da A", "1000.00");

        mvc.perform(get("/api/financeiro").headers(comoGateway(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("multi-tenant: nao da para alterar honorario de outra organizacao")
    void naoAlteraHonorarioDeOutraOrganizacao() throws Exception {
        Identidade dona = Identidade.nova();
        Identidade intrusa = Identidade.nova();
        String id = criar(dona, "Honorario da dona", "1000.00");

        mvc.perform(patch("/api/financeiro/{id}/status", id).headers(comoGateway(intrusa))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "PAGO"))))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/financeiro/{id}", id).headers(comoGateway(intrusa)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("marcar como PAGO preenche a data de pagamento e entra no recebido do resumo")
    void marcarComoPagoAtualizaResumo() throws Exception {
        Identidade id = Identidade.nova();
        String honorarioId = criar(id, "Honorario a receber", "700.00");

        mvc.perform(get("/api/financeiro/resumo").headers(comoGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emAberto").value(700.00))
                .andExpect(jsonPath("$.recebido").value(0));

        mvc.perform(patch("/api/financeiro/{id}/status", honorarioId).headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "PAGO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.paidAt").exists());

        mvc.perform(get("/api/financeiro/resumo").headers(comoGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emAberto").value(0))
                .andExpect(jsonPath("$.recebido").value(700.00));
    }

    @Test
    @DisplayName("secretaria nao acessa o financeiro")
    void secretariaNaoAcessa() throws Exception {
        mvc.perform(get("/api/financeiro").headers(comoGateway(Identidade.nova("SECRETARIA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("advogado e admin acessam o financeiro (hierarquia de papeis)")
    void advogadoEAdminAcessam() throws Exception {
        mvc.perform(get("/api/financeiro").headers(comoGateway(Identidade.nova("ADVOGADO"))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/financeiro").headers(comoGateway(Identidade.nova("ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("secretaria nao exclui honorario")
    void secretariaNaoExclui() throws Exception {
        Identidade admin = Identidade.nova();
        String id = criar(admin, "Honorario protegido", "100.00");

        mvc.perform(delete("/api/financeiro/{id}", id)
                        .headers(comoGateway(Identidade.mesmaOrg(admin, "SECRETARIA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("relatorio soma apenas os honorarios do periodo pedido")
    void relatorioRespeitaPeriodo() throws Exception {
        Identidade id = Identidade.nova();
        criar(id, "Honorario no periodo", "250.00");

        String inicio = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        String fim = Instant.now().plus(60, ChronoUnit.DAYS).toString();
        mvc.perform(get("/api/financeiro/relatorio").headers(comoGateway(id))
                        .param("start", inicio).param("end", fim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendente").value(250.00))
                .andExpect(jsonPath("$.totalPago").value(0));

        String depois = Instant.now().plus(180, ChronoUnit.DAYS).toString();
        String bemDepois = Instant.now().plus(200, ChronoUnit.DAYS).toString();
        mvc.perform(get("/api/financeiro/relatorio").headers(comoGateway(id))
                        .param("start", depois).param("end", bemDepois))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendente").value(0));
    }

    @Test
    @DisplayName("rota interna do portal exige a chave de servico")
    void rotaInternaExigeChave() throws Exception {
        String clientId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();

        mvc.perform(get("/internal/portal/honorarios").param("clientId", clientId).param("orgId", orgId))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/portal/honorarios").param("clientId", clientId).param("orgId", orgId)
                        .header("X-Internal-Key", "chave-errada"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/portal/honorarios").param("clientId", clientId).param("orgId", orgId)
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
