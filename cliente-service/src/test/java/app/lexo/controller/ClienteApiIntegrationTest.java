package app.lexo.controller;

import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API de clientes (integracao)")
class ClienteApiIntegrationTest {

    private static final String CHAVE_ASSINATURA = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";
    private static final String CHAVE_INTERNA = "chave-de-teste-interna";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private record Identidade(String userId, String orgId, String role, String name) {
        static Identidade nova() {
            return new Identidade(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    "ADMIN", "Admin Teste");
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
        h.set("X-User-Name", nome);
        h.set("X-User-Email", "");
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

    @Test
    @DisplayName("sem identidade, o acesso e bloqueado")
    void semIdentidadeBloqueia() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("headers de identidade sem assinatura do gateway sao recusados")
    void semAssinaturaBloqueia() throws Exception {
        mvc.perform(get("/api/clientes").headers(semAssinatura(Identidade.nova())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura feita com outra chave e recusada")
    void assinaturaDeOutraChaveBloqueia() throws Exception {
        HttpHeaders forjados = assinado(Identidade.nova(),
                "chave-de-atacante-com-mais-de-32-bytes-aqui", Instant.now().getEpochSecond());

        mvc.perform(get("/api/clientes").headers(forjados))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura fora da janela de tempo e recusada")
    void assinaturaExpiradaBloqueia() throws Exception {
        HttpHeaders antigos = assinado(Identidade.nova(), CHAVE_ASSINATURA,
                Instant.now().getEpochSecond() - 600);

        mvc.perform(get("/api/clientes").headers(antigos))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("trocar o papel depois de assinar invalida a identidade")
    void papelAdulteradoBloqueia() throws Exception {
        Identidade id = Identidade.nova();
        HttpHeaders h = comoGateway(id);
        h.set("X-User-Role", "SECRETARIA");

        mvc.perform(get("/api/clientes").headers(h))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("com identidade, cria e lista cliente")
    void criaEListaCliente() throws Exception {
        Identidade id = Identidade.nova();
        String cliente = om.writeValueAsString(Map.of("name", "Joao da Silva", "document", "52998224725"));

        mvc.perform(post("/api/clientes").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Joao da Silva"));

        mvc.perform(get("/api/clientes").headers(comoGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CPF invalido retorna 400 com mensagem e codigo de erro")
    void cpfInvalido() throws Exception {
        Identidade id = Identidade.nova();
        String cliente = om.writeValueAsString(Map.of("name", "Fulano", "document", "11111111111"));

        mvc.perform(post("/api/clientes").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("multi-tenant: uma organizacao nao ve clientes de outra")
    void isolamentoMultiTenant() throws Exception {
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();

        mvc.perform(post("/api/clientes").headers(comoGateway(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Cliente da A"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clientes").headers(comoGateway(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("rota interna exige a chave de servico")
    void rotaInternaExigeChave() throws Exception {
        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString())
                        .header("X-Internal-Key", "chave-errada"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }
}
