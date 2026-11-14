package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Case service security (integration)")
class CaseApiSecurityTest extends IntegrationTestBase {

    @Test
    @DisplayName("without identity, access is blocked")
    void noIdentity() throws Exception {
        mvc.perform(get("/api/processos")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("identity headers without the gateway signature are rejected")
    void unsignedHeaders() throws Exception {
        mvc.perform(get("/api/processos").headers(unsigned(Identity.create()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature made with another key is rejected")
    void otherKey() throws Exception {
        HttpHeaders forged = signedWith(Identity.create(), "chave-de-atacante-com-mais-de-32-bytes-aqui",
                Instant.now().getEpochSecond());

        mvc.perform(get("/api/processos").headers(forged)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature outside the time window is rejected")
    void expiredSignature() throws Exception {
        HttpHeaders old = signedWith(Identity.create(), SIGNING_KEY, Instant.now().getEpochSecond() - 600);

        mvc.perform(get("/api/processos").headers(old)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("changing the role after signing invalidates the identity")
    void tamperedRole() throws Exception {
        HttpHeaders headers = asGateway(Identity.create("SECRETARIA"));
        headers.set("X-User-Role", "ADMIN");

        mvc.perform(get("/api/processos").headers(headers)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/internal/processos/{id}/existe requires the internal key and answers per organization")
    void internalExists() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "SG-001");
        String url = "/internal/processos/" + caseId + "/existe";

        mvc.perform(get(url).param("orgId", id.orgId())).andExpect(status().isForbidden());
        mvc.perform(get(url).param("orgId", id.orgId()).header("X-Internal-Key", "chave-errada"))
                .andExpect(status().isForbidden());
        mvc.perform(get(url).param("orgId", id.orgId()).header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get(url).param("orgId", newId()).header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    @DisplayName("/internal/portal/processos gathers the client's cases, deadlines and movements")
    void internalPortal() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        String caseId = createCase(id, clientId, "SG-002");
        createDeadline(id, caseId, "AUDIENCIA", Duration.ofDays(8));
        postAs(id, "/api/processos/" + caseId + "/andamentos",
                Map.of("title", "Summons", "description", "summoned", "date", Instant.now().toString()))
                .andExpect(status().isCreated());
        createCase(id, newId(), "SG-003");

        mvc.perform(get("/internal/portal/processos").param("clientId", clientId).param("orgId", id.orgId()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/portal/processos").param("clientId", clientId).param("orgId", id.orgId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("SG-002"))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].prazos.length()").value(1))
                .andExpect(jsonPath("$[0].prazos[0].type").value("AUDIENCIA"))
                .andExpect(jsonPath("$[0].andamentos[0].title").value("Summons"));

        mvc.perform(get("/internal/portal/processos").param("clientId", clientId).param("orgId", newId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
