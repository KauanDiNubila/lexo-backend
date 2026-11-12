package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Organization isolation in the case service (integration)")
class OrganizationIsolationBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("an organization cannot see, change or delete the data of another")
    void organizationCannotReachAnotherOrganizationData() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String clientId = newId();
        String caseId = createCase(a, clientId, "IS-001");
        String deadlineId = createDeadline(a, caseId, "PRAZO", Duration.ofDays(5));
        String movementId = createMovement(a, caseId, "Order", Instant.now());

        getAs(b, "/api/processos").andExpect(jsonPath("$.totalElements").value(0));
        getAs(b, "/api/processos/opcoes").andExpect(jsonPath("$.length()").value(0));
        getAs(b, "/api/processos/resumo").andExpect(jsonPath("$.total").value(0));
        getAs(b, "/api/processos/" + caseId).andExpect(status().isNotFound());
        putAs(b, "/api/processos/" + caseId, caseData(clientId, "IS-HACK")).andExpect(status().isNotFound());
        deleteAs(b, "/api/processos/" + caseId).andExpect(status().isNotFound());

        getAs(b, "/api/agenda").andExpect(jsonPath("$.totalElements").value(0));
        getAs(b, "/api/agenda?caseId=" + caseId).andExpect(jsonPath("$.totalElements").value(0));
        getAs(b, "/api/agenda/resumo").andExpect(jsonPath("$.proximos.length()").value(0));
        toggleStatus(b, deadlineId, true).andExpect(status().isNotFound());
        putAs(b, "/api/agenda/" + deadlineId, Map.of("caseId", caseId, "title", "x", "type", "PRAZO",
                "date", Instant.now().toString())).andExpect(status().isNotFound());
        deleteAs(b, "/api/agenda/" + deadlineId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Prazo n\u00e3o encontrado"));

        getAs(b, "/api/processos/" + caseId + "/andamentos").andExpect(status().isNotFound());
        deleteAs(b, "/api/processos/" + caseId + "/andamentos/" + movementId).andExpect(status().isNotFound());

        getAs(a, "/api/processos/" + caseId).andExpect(jsonPath("$.number").value("IS-001"));
        getAs(a, "/api/agenda")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
        getAs(a, "/api/processos/" + caseId + "/andamentos").andExpect(jsonPath("$.length()").value(1));
    }
}
