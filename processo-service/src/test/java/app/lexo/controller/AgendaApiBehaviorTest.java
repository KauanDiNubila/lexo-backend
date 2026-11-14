package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Agenda API (integration)")
class AgendaApiBehaviorTest extends IntegrationTestBase {

    private Map<String, Object> deadlineData(String caseId, String title, String type, Duration when) {
        return Map.of("caseId", caseId, "title", title, "type", type, "date", Instant.now().plus(when).toString());
    }

    @Test
    @DisplayName("creates, updates, completes, reopens and deletes a deadline")
    void lifecycle() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AG-001");

        String deadlineId = idOf(postAs(id, "/api/agenda",
                deadlineData(caseId, "Respond", "PRAZO", Duration.ofDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.caseId").value(caseId)));

        putAs(id, "/api/agenda/" + deadlineId,
                deadlineData(caseId, "Respond with documents", "AUDIENCIA", Duration.ofDays(20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Respond with documents"))
                .andExpect(jsonPath("$.type").value("AUDIENCIA"));

        toggleStatus(id, deadlineId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.risk").doesNotExist());
        toggleStatus(id, deadlineId, false)
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.risk").exists());

        deleteAs(id, "/api/agenda/" + deadlineId).andExpect(status().isNoContent());
        getAs(id, "/api/agenda").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("a deadline for a case of another organization returns 404")
    void caseOfAnotherOrganization() throws Exception {
        Identity owner = Identity.create();
        Identity other = Identity.create();
        String caseId = createCase(owner, newId(), "AG-002");

        postAs(other, "/api/agenda", deadlineData(caseId, "Intrusion", "PRAZO", Duration.ofDays(3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Processo não encontrado"));
    }

    @Test
    @DisplayName("a deadline without title or date returns 400")
    void validation() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AG-003");

        postAs(id, "/api/agenda", Map.of("caseId", caseId, "title", "", "type", "PRAZO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("lists all deadlines of the organization, or only those of one case, nearest first")
    void listing() throws Exception {
        Identity id = Identity.create();
        String first = createCase(id, newId(), "AG-004");
        String second = createCase(id, newId(), "AG-005");
        createDeadline(id, first, "PRAZO", Duration.ofDays(30));
        createDeadline(id, first, "PRAZO", Duration.ofDays(5));
        createDeadline(id, second, "REUNIAO", Duration.ofDays(9));

        getAs(id, "/api/agenda")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].caseId").value(first))
                .andExpect(jsonPath("$.content[2].caseId").value(first));

        getAs(id, "/api/agenda?caseId=" + second)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("REUNIAO"));
    }

    @Test
    @DisplayName("risk depends on how near the date is and on the deadline type")
    void risk() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AG-006");
        String criticalByHours = createDeadline(id, caseId, "PRAZO", Duration.ofHours(12));
        String urgent = createDeadline(id, caseId, "PRAZO", Duration.ofDays(2));
        String highByFactor = createDeadline(id, caseId, "PRAZO", Duration.ofDays(10));
        String mediumWithoutFactor = createDeadline(id, caseId, "OUTRO", Duration.ofDays(10));
        String low = createDeadline(id, caseId, "AUDIENCIA", Duration.ofDays(30));
        String late = createDeadline(id, caseId, "PRAZO", Duration.ofDays(-3));

        assertRisk(id, criticalByHours, "CRITICO");
        assertRisk(id, urgent, "URGENTE");
        assertRisk(id, highByFactor, "ALTO");
        assertRisk(id, mediumWithoutFactor, "MEDIO");
        assertRisk(id, low, "BAIXO");
        assertRisk(id, late, "CRITICO");
    }

    private void assertRisk(Identity id, String deadlineId, String expected) throws Exception {
        var content = bodyOf(getAs(id, "/api/agenda?size=50").andExpect(status().isOk())).get("content");
        for (var deadline : content) {
            if (deadlineId.equals(deadline.get("id").asText())) {
                assertThat(deadline.get("risk").asText()).isEqualTo(expected);
                return;
            }
        }
        fail("deadline %s not found in the agenda", deadlineId);
    }

    @Test
    @DisplayName("summary counts urgent (7 days) and overdue, and returns the 5 nearest pending")
    void summary() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "AG-007");
        String overdue = createDeadline(id, caseId, "PRAZO", Duration.ofDays(-3));
        String next = createDeadline(id, caseId, "PRAZO", Duration.ofDays(2));
        createDeadline(id, caseId, "PRAZO", Duration.ofDays(5));
        createDeadline(id, caseId, "PRAZO", Duration.ofDays(20));
        String completed = createDeadline(id, caseId, "PRAZO", Duration.ofDays(1));
        toggleStatus(id, completed, true);

        getAs(id, "/api/agenda/resumo")
                .andExpect(jsonPath("$.urgentes").value(2))
                .andExpect(jsonPath("$.vencidos").value(1))
                .andExpect(jsonPath("$.proximos.length()").value(4))
                .andExpect(jsonPath("$.proximos[0].id").value(overdue))
                .andExpect(jsonPath("$.proximos[1].id").value(next));
    }
}
