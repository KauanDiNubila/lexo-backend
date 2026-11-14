package app.lexo.application.service;

import app.lexo.domain.RefreshToken;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurgeRefreshTokensService")
class PurgeRefreshTokensServiceTest {

    private final InMemoryAuth store = new InMemoryAuth();
    private final PurgeRefreshTokensService purge = new PurgeRefreshTokensService(store, Fakes.CLOCK);
    private final UserId user = new UserId("user-1");

    private RefreshToken expiringDaysFromNow(long days, String hash) {
        return store.save(RefreshToken.issue(user, hash, Fakes.NOW.plus(days, ChronoUnit.DAYS)));
    }

    @Test
    @DisplayName("tokens expired for more than seven days are removed, newer ones are kept")
    void removesOnlyLongExpiredTokens() {
        expiringDaysFromNow(-8, "long-expired");
        expiringDaysFromNow(-6, "recently-expired");
        expiringDaysFromNow(10, "active");

        int purged = purge.purge();

        assertThat(purged).isEqualTo(1);
        assertThat(store.tokens.values()).extracting(RefreshToken::tokenHash)
                .containsExactlyInAnyOrder("recently-expired", "active");
    }

    @Test
    @DisplayName("a revoked token that has not expired for long enough is kept for reuse detection")
    void keepsRevokedTokensUntilTheyAreOldEnough() {
        RefreshToken revoked = expiringDaysFromNow(-2, "revoked");
        revoked.revoke(Fakes.NOW.minusSeconds(3600));
        store.save(revoked);

        assertThat(purge.purge()).isZero();
        assertThat(store.tokens).hasSize(1);
    }

    @Test
    @DisplayName("purging with nothing to remove returns zero")
    void nothingToRemove() {
        assertThat(purge.purge()).isZero();
    }
}
