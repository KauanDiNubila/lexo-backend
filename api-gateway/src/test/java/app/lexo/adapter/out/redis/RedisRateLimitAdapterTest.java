package app.lexo.adapter.out.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Redis rate limit adapter")
class RedisRateLimitAdapterTest {

    @Test
    @DisplayName("the rate limiter is configured with the three values and answers per client")
    void rateLimit() {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        Map<String, RedisRateLimiter.Config> configs = new HashMap<>();
        when(limiter.getConfig()).thenReturn(configs);
        when(limiter.isAllowed("gateway-global", "10.0.0.1"))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
        when(limiter.isAllowed("gateway-global", "10.0.0.2"))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of())));

        RedisRateLimitAdapter adapter = new RedisRateLimitAdapter(limiter, 30, 60, 1);

        RedisRateLimiter.Config config = configs.get("gateway-global");
        assertThat(config.getReplenishRate()).isEqualTo(30);
        assertThat(config.getBurstCapacity()).isEqualTo(60);
        assertThat(config.getRequestedTokens()).isEqualTo(1);
        assertThat(adapter.isAllowed("10.0.0.1").block()).isTrue();
        assertThat(adapter.isAllowed("10.0.0.2").block()).isFalse();
    }
}
