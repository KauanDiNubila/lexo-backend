package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deadline risk")
class DeadlineRiskTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    private static Deadline deadline(DeadlineType type, DeadlineStatus status, Duration fromNow) {
        DeadlineData data = new DeadlineData(new CaseId("case-1"), "Respond", type, NOW.plus(fromNow), null);
        return Deadline.restore(new DeadlineId("d-1"), new OrganizationId("org-1"), data, status, null);
    }

    private static Optional<RiskLevel> risk(DeadlineType type, Duration fromNow) {
        return deadline(type, DeadlineStatus.PENDING, fromNow).riskAt(NOW);
    }

    @Test
    @DisplayName("a deadline that is not pending has no risk")
    void notPendingHasNoRisk() {
        assertThat(deadline(DeadlineType.DEADLINE, DeadlineStatus.COMPLETED, Duration.ZERO).riskAt(NOW)).isEmpty();
        assertThat(deadline(DeadlineType.DEADLINE, DeadlineStatus.MISSED, Duration.ZERO).riskAt(NOW)).isEmpty();
    }

    @Test
    @DisplayName("a very near date is critical, and so is a date already past")
    void critical() {
        assertThat(risk(DeadlineType.OTHER, Duration.ofHours(12))).contains(RiskLevel.CRITICAL);
        assertThat(risk(DeadlineType.DEADLINE, Duration.ofDays(-3))).contains(RiskLevel.CRITICAL);
    }

    @Test
    @DisplayName("DEADLINE weighs more than OTHER because of its factor 1.5")
    void deadlineWeighsMore() {
        assertThat(risk(DeadlineType.DEADLINE, Duration.ofDays(2))).contains(RiskLevel.URGENT);
        assertThat(risk(DeadlineType.OTHER, Duration.ofDays(2))).contains(RiskLevel.URGENT);
        assertThat(risk(DeadlineType.DEADLINE, Duration.ofDays(10))).contains(RiskLevel.HIGH);
        assertThat(risk(DeadlineType.OTHER, Duration.ofDays(10))).contains(RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("the scale by nearness is HIGH, MEDIUM and LOW")
    void scale() {
        assertThat(risk(DeadlineType.OTHER, Duration.ofDays(5))).contains(RiskLevel.HIGH);
        assertThat(risk(DeadlineType.OTHER, Duration.ofDays(10))).contains(RiskLevel.MEDIUM);
        assertThat(risk(DeadlineType.OTHER, Duration.ofDays(100))).contains(RiskLevel.LOW);
        assertThat(risk(DeadlineType.HEARING, Duration.ofDays(30))).contains(RiskLevel.LOW);
    }

    @Test
    @DisplayName("risk codes are the values the API returns")
    void codes() {
        assertThat(RiskLevel.values()).extracting(RiskLevel::code)
                .containsExactly("CRITICO", "URGENTE", "ALTO", "MEDIO", "BAIXO");
    }
}
