package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken")
class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    @DisplayName("a new token is neither revoked nor expired")
    void startsUsable() {
        RefreshToken token = RefreshToken.issue(new UserId("u1"), "hash", NOW.plusSeconds(60));

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpiredAt(NOW)).isFalse();
    }

    @Test
    @DisplayName("it expires exactly at its expiry instant")
    void expiresAtTheBoundary() {
        RefreshToken token = RefreshToken.issue(new UserId("u1"), "hash", NOW);

        assertThat(token.isExpiredAt(NOW.minusMillis(1))).isFalse();
        assertThat(token.isExpiredAt(NOW)).isTrue();
    }

    @Test
    @DisplayName("revoking records when it happened")
    void revokeRecordsTheInstant() {
        RefreshToken token = RefreshToken.issue(new UserId("u1"), "hash", NOW.plusSeconds(60));

        token.revoke(NOW);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.revokedAt()).isEqualTo(NOW);
    }
}
