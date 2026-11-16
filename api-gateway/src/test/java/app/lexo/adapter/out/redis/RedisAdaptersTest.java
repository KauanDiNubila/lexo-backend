package app.lexo.adapter.out.redis;

import app.lexo.common.signing.SessionCutoff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Redis adapters")
class RedisAdaptersTest {

    @SuppressWarnings("unchecked")
    private final ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    private final ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
    private final RedisSessionCutoffAdapter cutoffs = new RedisSessionCutoffAdapter(template);

    RedisAdaptersTest() {
        when(template.opsForValue()).thenReturn(values);
    }

    @Test
    @DisplayName("the cutoff is read from the user's key and parsed as seconds")
    void readsTheCutoff() {
        when(values.get(SessionCutoff.key("u-1"))).thenReturn(Mono.just("1700000000"));

        assertThat(cutoffs.cutoffOf("u-1").block()).isEqualTo(1_700_000_000L);
    }

    @Test
    @DisplayName("a user without a cutoff gives an empty result")
    void noCutoff() {
        when(values.get(SessionCutoff.key("u-1"))).thenReturn(Mono.empty());

        assertThat(cutoffs.cutoffOf("u-1").block()).isNull();
    }

    @Test
    @DisplayName("a Redis failure and a value that is not a number both reach the caller as errors")
    void failures() {
        when(values.get(SessionCutoff.key("u-1"))).thenReturn(Mono.error(new IllegalStateException("fora do ar")));
        when(values.get(SessionCutoff.key("u-2"))).thenReturn(Mono.just("abc"));

        assertThatThrownBy(() -> cutoffs.cutoffOf("u-1").block()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cutoffs.cutoffOf("u-2").block()).isInstanceOf(NumberFormatException.class);
    }

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
