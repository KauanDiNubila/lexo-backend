package app.lexo.controller;

import app.lexo.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Seguranca do processo-service (integracao)")
class SegurancaApiIntegrationTest extends IntegracaoBase {

    @Test
    @DisplayName("sem identidade, o acesso e bloqueado")
    void semIdentidade() throws Exception {
        mvc.perform(get("/api/processos")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("headers de identidade sem assinatura do gateway sao recusados")
    void semAssinatura() throws Exception {
        mvc.perform(get("/api/processos").headers(semAssinatura(Identidade.nova())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura feita com outra chave e recusada")
    void outraChave() throws Exception {
        HttpHeaders forjados = assinado(Identidade.nova(), "chave-de-atacante-com-mais-de-32-bytes-aqui",
                Instant.now().getEpochSecond());

        mvc.perform(get("/api/processos").headers(forjados)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assinatura fora da janela de tempo e recusada")
    void assinaturaExpirada() throws Exception {
        HttpHeaders antigos = assinado(Identidade.nova(), CHAVE_ASSINATURA, Instant.now().getEpochSecond() - 600);

        mvc.perform(get("/api/processos").headers(antigos)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("trocar o papel depois de assinar invalida a identidade")
    void papelAdulterado() throws Exception {
        HttpHeaders h = comoGateway(Identidade.nova("SECRETARIA"));
        h.set("X-User-Role", "ADMIN");

        mvc.perform(get("/api/processos").headers(h)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/internal/processos/{id}/existe exige a chave interna e responde por escritorio")
    void existeInterno() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "SG-001");
        String url = "/internal/processos/" + processoId + "/existe";

        mvc.perform(get(url).param("orgId", id.orgId())).andExpect(status().isForbidden());
        mvc.perform(get(url).param("orgId", id.orgId()).header("X-Internal-Key", "chave-errada"))
                .andExpect(status().isForbidden());
        mvc.perform(get(url).param("orgId", id.orgId()).header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get(url).param("orgId", novoId()).header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    @DisplayName("/internal/portal/processos agrega processos, prazos e andamentos do cliente")
    void portalInterno() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = novoId();
        String processoId = criarProcesso(id, clienteId, "SG-002");
        criarPrazo(id, processoId, "AUDIENCIA", Duration.ofDays(8));
        postar(id, "/api/processos/" + processoId + "/andamentos",
                Map.of("title", "Citacao", "description", "citado", "date", Instant.now().toString()))
                .andExpect(status().isCreated());
        criarProcesso(id, novoId(), "SG-003");

        mvc.perform(get("/internal/portal/processos").param("clientId", clienteId).param("orgId", id.orgId()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/portal/processos").param("clientId", clienteId).param("orgId", id.orgId())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("SG-002"))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].prazos.length()").value(1))
                .andExpect(jsonPath("$[0].prazos[0].type").value("AUDIENCIA"))
                .andExpect(jsonPath("$[0].andamentos[0].title").value("Citacao"));
        mvc.perform(get("/internal/portal/processos").param("clientId", clienteId).param("orgId", novoId())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
