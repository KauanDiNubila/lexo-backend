package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deadline")
class DeadlineTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");

    private static DeadlineData data(DeadlineType type, Duration fromNow) {
        return new DeadlineData(new CaseId("case-1"), "Respond", type, NOW.plus(fromNow), null);
    }

    @Test
    @DisplayName("a scheduled deadline starts pending and not notified")
    void schedule() {
        Deadline deadline = Deadline.schedule(ORGANIZATION, data(DeadlineType.DEADLINE, Duration.ofDays(5)));

        assertThat(deadline.id()).isNull();
        assertThat(deadline.status()).isEqualTo(DeadlineStatus.PENDING);
        assertThat(deadline.notifiedAt()).isNull();
    }

    @Test
    @DisplayName("completing and reopening change only the status; reopening also rescues a missed deadline")
    void completeAndReopen() {
        Deadline deadline = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION,
                data(DeadlineType.DEADLINE, Duration.ofDays(5)), DeadlineStatus.MISSED, NOW);

        deadline.reopen();
        assertThat(deadline.status()).isEqualTo(DeadlineStatus.PENDING);

        deadline.complete();
        assertThat(deadline.status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(deadline.notifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("updating without changing the date keeps the status and the notification mark")
    void updateKeepsNotificationWhenTheDateStays() {
        Deadline deadline = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION,
                data(DeadlineType.DEADLINE, Duration.ofDays(1)), DeadlineStatus.COMPLETED, NOW);

        deadline.update(data(DeadlineType.HEARING, Duration.ofDays(1)));

        assertThat(deadline.data().type()).isEqualTo(DeadlineType.HEARING);
        assertThat(deadline.status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(deadline.notifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("moving the date clears the notification mark and keeps the status")
    void updateRearmsReminderWhenTheDateChanges() {
        Deadline deadline = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION,
                data(DeadlineType.DEADLINE, Duration.ofDays(1)), DeadlineStatus.PENDING, NOW);

        deadline.update(data(DeadlineType.DEADLINE, Duration.ofDays(30)));

        assertThat(deadline.notifiedAt()).isNull();
        assertThat(deadline.status()).isEqualTo(DeadlineStatus.PENDING);
    }

    @Test
    @DisplayName("marking as notified records the moment")
    void markNotified() {
        Deadline deadline = Deadline.schedule(ORGANIZATION, data(DeadlineType.DEADLINE, Duration.ofDays(1)));

        deadline.markNotified(NOW);

        assertThat(deadline.notifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("a blank description becomes absent")
    void blankDescription() {
        assertThat(new DeadlineData(new CaseId("c"), "t", DeadlineType.OTHER, NOW, "  ").description()).isNull();
    }
}
