package app.lexo.application.port.in;

import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.MovementData;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Responses")
class ResponsesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");

    @Test
    @DisplayName("a case response carries the stored data, with an empty responsible as null")
    void caseResponse() {
        CaseData data = new CaseData(new ClientId("c-1"), "0001", "Civel", CaseStatus.ACTIVE, null, new UserId("u-1"));
        Case restored = Case.restore(new CaseId("case-1"), ORGANIZATION, data, NOW);

        CaseResponse response = CaseResponse.of(restored);

        assertThat(response.id()).isEqualTo("case-1");
        assertThat(response.clientId()).isEqualTo("c-1");
        assertThat(response.responsibleId()).isEqualTo("u-1");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(CaseResponse.of(Case.restore(new CaseId("c2"), ORGANIZATION,
                new CaseData(new ClientId("c-1"), "2", null, CaseStatus.CLOSED, null, null), NOW)).responsibleId())
                .isNull();
    }

    @Test
    @DisplayName("a pending deadline response carries the risk code; a completed one carries none")
    void deadlineResponse() {
        DeadlineData data = new DeadlineData(new CaseId("case-1"), "t", DeadlineType.DEADLINE,
                NOW.plus(Duration.ofDays(2)), null);
        Deadline pending = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION, data, DeadlineStatus.PENDING, null);
        Deadline completed = Deadline.restore(new DeadlineId("d-2"), ORGANIZATION, data, DeadlineStatus.COMPLETED, null);

        assertThat(DeadlineResponse.of(pending, NOW).risk()).isEqualTo("URGENTE");
        assertThat(DeadlineResponse.of(completed, NOW).risk()).isNull();
    }

    @Test
    @DisplayName("a movement response carries the case id and the creation time")
    void movementResponse() {
        CaseMovement movement = CaseMovement.restore(new MovementId("m-1"), ORGANIZATION, new CaseId("case-1"),
                new MovementData("title", "text", NOW), NOW);

        MovementResponse response = MovementResponse.of(movement);

        assertThat(response.caseId()).isEqualTo("case-1");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }
}
