package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Movements API (integration)")
class MovementApiBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("lists from the most recent movement to the oldest")
    void listsByDateDescending() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AN-001");
        Instant now = Instant.now();
        createMovement(id, caseId, "Distribution", now.minus(10, ChronoUnit.DAYS));
        createMovement(id, caseId, "Sentence", now);
        createMovement(id, caseId, "Summons", now.minus(5, ChronoUnit.DAYS));

        getAs(id, "/api/processos/" + caseId + "/andamentos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Sentence"))
                .andExpect(jsonPath("$[1].title").value("Summons"))
                .andExpect(jsonPath("$[2].title").value("Distribution"))
                .andExpect(jsonPath("$[0].description").value("details of Sentence"));
    }

    @Test
    @DisplayName("deletes a movement")
    void deletes() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AN-002");
        String movementId = createMovement(id, caseId, "Attachment", Instant.now());

        deleteAs(id, "/api/processos/" + caseId + "/andamentos/" + movementId).andExpect(status().isNoContent());

        getAs(id, "/api/processos/" + caseId + "/andamentos").andExpect(jsonPath("$.length()").value(0));
        deleteAs(id, "/api/processos/" + caseId + "/andamentos/" + movementId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Andamento não encontrado"));
    }

    @Test
    @DisplayName("without title or date returns 400")
    void validation() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AN-003");

        postAs(id, "/api/processos/" + caseId + "/andamentos", Map.of("title", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("a case of another organization returns 404 on list, create and delete")
    void caseOfAnotherOrganization() throws Exception {
        Identity owner = Identity.create();
        Identity other = Identity.create();
        String caseId = createCase(owner, newId(), "AN-004");
        String movementId = createMovement(owner, caseId, "Order", Instant.now());

        getAs(other, "/api/processos/" + caseId + "/andamentos").andExpect(status().isNotFound());
        postAs(other, "/api/processos/" + caseId + "/andamentos",
                Map.of("title", "Intrusion", "date", Instant.now().toString())).andExpect(status().isNotFound());
        deleteAs(other, "/api/processos/" + caseId + "/andamentos/" + movementId).andExpect(status().isNotFound());

        getAs(owner, "/api/processos/" + caseId + "/andamentos").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("deleting through the URL of another case of the same organization returns 404 and keeps the movement")
    void deleteChecksCaseInPath() throws Exception {
        Identity id = Identity.create();
        String firstCase = createCase(id, newId(), "AN-005");
        String secondCase = createCase(id, newId(), "AN-006");
        String movementId = createMovement(id, firstCase, "Hearing", Instant.now());

        deleteAs(id, "/api/processos/" + secondCase + "/andamentos/" + movementId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Andamento n\u00e3o encontrado"));

        getAs(id, "/api/processos/" + firstCase + "/andamentos").andExpect(jsonPath("$.length()").value(1));
    }
}
