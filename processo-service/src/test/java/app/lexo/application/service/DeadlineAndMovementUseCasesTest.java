package app.lexo.application.service;

import app.lexo.application.port.in.ChangeDeadlineStatusCommand;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.DeleteDeadlineCommand;
import app.lexo.application.port.in.DeleteMovementCommand;
import app.lexo.application.port.in.MovementResponse;
import app.lexo.application.port.in.RecordMovementCommand;
import app.lexo.application.port.in.ScheduleDeadlineCommand;
import app.lexo.application.port.in.UpdateDeadlineCommand;
import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineNotFoundException;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.MovementData;
import app.lexo.domain.MovementId;
import app.lexo.domain.MovementNotFoundException;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deadline and movement use cases (in-memory ports)")
class DeadlineAndMovementUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");
    private static final OrganizationId OTHER_ORGANIZATION = new OrganizationId("org-2");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryCases cases = new InMemoryCases();
    private final InMemoryDeadlines deadlines = new InMemoryDeadlines();
    private final InMemoryMovements movements = new InMemoryMovements();
    private final CaseGuard guard = new CaseGuard(cases);
    private final ScheduleDeadlineService schedule = new ScheduleDeadlineService(guard, deadlines, clock);
    private final UpdateDeadlineService update = new UpdateDeadlineService(guard, deadlines, deadlines, clock);
    private final ChangeDeadlineStatusService changeStatus =
            new ChangeDeadlineStatusService(deadlines, deadlines, clock);
    private final DeleteDeadlineService deleteDeadline = new DeleteDeadlineService(deadlines);
    private final RecordMovementService record = new RecordMovementService(guard, movements);
    private final DeleteMovementService deleteMovement = new DeleteMovementService(guard, movements);

    private CaseId openCase(String number) {
        return cases.save(Case.open(ORGANIZATION,
                new CaseData(new ClientId("client-1"), number, null, CaseStatus.ACTIVE, null, null))).id();
    }

    private DeadlineData deadlineData(CaseId caseId, DeadlineType type, Duration fromNow) {
        return new DeadlineData(caseId, "Respond", type, NOW.plus(fromNow), null);
    }

    @Test
    @DisplayName("scheduling needs an existing case of the organization and starts pending with a risk")
    void schedules() {
        CaseId caseId = openCase("D-1");

        DeadlineResponse response = schedule.schedule(new ScheduleDeadlineCommand(ORGANIZATION,
                deadlineData(caseId, DeadlineType.DEADLINE, Duration.ofDays(2))));

        assertThat(response.status()).isEqualTo(DeadlineStatus.PENDING);
        assertThat(response.risk()).isEqualTo("URGENTE");
        assertThatThrownBy(() -> schedule.schedule(new ScheduleDeadlineCommand(OTHER_ORGANIZATION,
                deadlineData(caseId, DeadlineType.DEADLINE, Duration.ofDays(2)))))
                .isInstanceOf(CaseNotFoundException.class);
        assertThat(deadlines.all()).hasSize(1);
    }

    @Test
    @DisplayName("updating keeps the status and the notification mark, and needs the case and the deadline")
    void updates() {
        CaseId caseId = openCase("D-2");
        Deadline saved = deadlines.save(Deadline.restore(null, ORGANIZATION,
                deadlineData(caseId, DeadlineType.DEADLINE, Duration.ofDays(1)), DeadlineStatus.COMPLETED, NOW));

        DeadlineResponse response = update.update(new UpdateDeadlineCommand(ORGANIZATION, saved.id(),
                deadlineData(caseId, DeadlineType.HEARING, Duration.ofDays(30))));

        assertThat(response.type()).isEqualTo(DeadlineType.HEARING);
        assertThat(response.status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(deadlines.all().get(0).notifiedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> update.update(new UpdateDeadlineCommand(ORGANIZATION, new DeadlineId("ghost"),
                deadlineData(caseId, DeadlineType.HEARING, Duration.ofDays(1)))))
                .isInstanceOf(DeadlineNotFoundException.class);
        assertThatThrownBy(() -> update.update(new UpdateDeadlineCommand(ORGANIZATION, saved.id(),
                deadlineData(new CaseId("ghost"), DeadlineType.HEARING, Duration.ofDays(1)))))
                .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    @DisplayName("completing removes the risk and reopening brings it back")
    void changesStatus() {
        CaseId caseId = openCase("D-3");
        DeadlineId id = deadlines.save(Deadline.schedule(ORGANIZATION,
                deadlineData(caseId, DeadlineType.DEADLINE, Duration.ofDays(2)))).id();

        DeadlineResponse completed = changeStatus.changeStatus(new ChangeDeadlineStatusCommand(ORGANIZATION, id, true));
        DeadlineResponse reopened = changeStatus.changeStatus(new ChangeDeadlineStatusCommand(ORGANIZATION, id, false));

        assertThat(completed.status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(completed.risk()).isNull();
        assertThat(reopened.status()).isEqualTo(DeadlineStatus.PENDING);
        assertThat(reopened.risk()).isNotNull();
        assertThatThrownBy(() -> changeStatus.changeStatus(
                new ChangeDeadlineStatusCommand(OTHER_ORGANIZATION, id, true)))
                .isInstanceOf(DeadlineNotFoundException.class);
    }

    @Test
    @DisplayName("deleting a deadline never fails, even for one that does not exist or belongs to another organization")
    void deletesSilently() {
        CaseId caseId = openCase("D-4");
        DeadlineId id = deadlines.save(Deadline.schedule(ORGANIZATION,
                deadlineData(caseId, DeadlineType.OTHER, Duration.ofDays(5)))).id();

        deleteDeadline.delete(new DeleteDeadlineCommand(OTHER_ORGANIZATION, id));
        deleteDeadline.delete(new DeleteDeadlineCommand(ORGANIZATION, new DeadlineId("ghost")));
        assertThat(deadlines.all()).hasSize(1);

        deleteDeadline.delete(new DeleteDeadlineCommand(ORGANIZATION, id));
        assertThat(deadlines.all()).isEmpty();
    }

    @Test
    @DisplayName("recording a movement needs an existing case of the organization")
    void recordsMovement() {
        CaseId caseId = openCase("M-1");
        MovementData data = new MovementData("Order", "text", NOW);

        MovementResponse response = record.record(new RecordMovementCommand(ORGANIZATION, caseId, data));

        assertThat(response.caseId()).isEqualTo(caseId.value());
        assertThatThrownBy(() -> record.record(new RecordMovementCommand(OTHER_ORGANIZATION, caseId, data)))
                .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    @DisplayName("deleting a movement fails for an unknown movement or case, and ignores which case is in the path")
    void deletesMovement() {
        CaseId first = openCase("M-2");
        CaseId second = openCase("M-3");
        MovementId id = movements.save(CaseMovement.record(ORGANIZATION, first,
                new MovementData("Hearing", null, NOW))).id();

        assertThatThrownBy(() -> deleteMovement.delete(new DeleteMovementCommand(ORGANIZATION, first,
                new MovementId("ghost")))).isInstanceOf(MovementNotFoundException.class);
        assertThatThrownBy(() -> deleteMovement.delete(new DeleteMovementCommand(ORGANIZATION,
                new CaseId("ghost"), id))).isInstanceOf(CaseNotFoundException.class);

        deleteMovement.delete(new DeleteMovementCommand(ORGANIZATION, second, id));

        assertThat(movements.all()).isEmpty();
    }
}
