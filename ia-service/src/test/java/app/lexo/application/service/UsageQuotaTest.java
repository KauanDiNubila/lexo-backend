package app.lexo.application.service;

import app.lexo.application.service.Fakes.RateLimit;
import app.lexo.domain.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static app.lexo.application.service.Fakes.ORGANIZATION;
import static app.lexo.application.service.Fakes.SETTINGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsageQuota")
class UsageQuotaTest {

    private final RateLimit rateLimit = new RateLimit();
    private final UsageQuota quota = new UsageQuota(rateLimit, SETTINGS);

    @Test
    @DisplayName("registers one use for the organization against the configured limit in a one minute window")
    void registersOneUse() {
        quota.spend(ORGANIZATION);

        assertThat(rateLimit.calls).containsExactly(
                new RateLimit.Call(ORGANIZATION, 3, Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("refuses when the limit port says the limit was passed")
    void refusesWhenOverTheLimit() {
        rateLimit.allow = false;

        assertThatThrownBy(() -> quota.spend(ORGANIZATION)).isInstanceOf(RateLimitExceededException.class);
    }
}
