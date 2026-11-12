package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.MovementData;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Command validation")
class CommandValidationTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");
    private static final Actor ACTOR = new Actor("u-1", "Ana");

    private static CaseData caseData(String number, CaseStatus status) {
        return new CaseData(new ClientId("c-1"), number, null, status, null, null);
    }

    @Test
    @DisplayName("a case needs a number and a status")
    void caseNeedsNumberAndStatus() {
        assertThatThrownBy(() -> new CreateCaseCommand(ORGANIZATION, ACTOR, caseData(" ", CaseStatus.ACTIVE)))
                .isInstanceOf(InvalidDataException.class).hasMessage("N\u00famero do processo \u00e9 obrigat\u00f3rio");
        assertThatThrownBy(() -> new UpdateCaseCommand(ORGANIZATION, new CaseId("x"), ACTOR, caseData("1", null)))
                .isInstanceOf(InvalidDataException.class).hasMessage("Status \u00e9 obrigat\u00f3rio");
        assertThat(new CreateCaseCommand(ORGANIZATION, ACTOR, caseData("1", CaseStatus.ACTIVE)).data().number())
                .isEqualTo("1");
    }

    @Test
    @DisplayName("a deadline needs a title, a type and a date")
    void deadlineNeedsTitleTypeAndDate() {
        CaseId caseId = new CaseId("x");
        Instant date = Instant.parse("2026-06-01T00:00:00Z");

        assertThatThrownBy(() -> new ScheduleDeadlineCommand(ORGANIZATION,
                new DeadlineData(caseId, "", DeadlineType.DEADLINE, date, null)))
                .isInstanceOf(InvalidDataException.class).hasMessage("T\u00edtulo \u00e9 obrigat\u00f3rio");
        assertThatThrownBy(() -> new ScheduleDeadlineCommand(ORGANIZATION,
                new DeadlineData(caseId, "t", null, date, null)))
                .isInstanceOf(InvalidDataException.class).hasMessage("Tipo \u00e9 obrigat\u00f3rio");
        assertThatThrownBy(() -> new ScheduleDeadlineCommand(ORGANIZATION,
                new DeadlineData(caseId, "t", DeadlineType.DEADLINE, null, null)))
                .isInstanceOf(InvalidDataException.class).hasMessage("Data \u00e9 obrigat\u00f3ria");
    }

    @Test
    @DisplayName("a movement needs a title and a date")
    void movementNeedsTitleAndDate() {
        assertThatThrownBy(() -> new RecordMovementCommand(ORGANIZATION, new CaseId("x"),
                new MovementData(" ", null, Instant.now())))
                .isInstanceOf(InvalidDataException.class).hasMessage("Descreva o andamento");
        assertThatThrownBy(() -> new RecordMovementCommand(ORGANIZATION, new CaseId("x"),
                new MovementData("t", null, null)))
                .isInstanceOf(InvalidDataException.class).hasMessage("Informe a data");
    }
}
