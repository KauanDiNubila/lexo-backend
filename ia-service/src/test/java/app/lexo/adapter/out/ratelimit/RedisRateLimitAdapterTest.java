package app.lexo.adapter.out.ratelimit;

import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RedisRateLimitAdapter")
class RedisRateLimitAdapterTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisRateLimitAdapter adapter = new RedisRateLimitAdapter(redis);

    private void counter(Long count) {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenReturn(count);
    }

    @Test
    @DisplayName("counts in the ratelimit:ia key of the organization with the window in seconds")
    void key() {
        counter(1L);

        adapter.register(ORGANIZATION, 3, Duration.ofMinutes(1));

        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:ia:escritorio-1")),
                eq("60"));
    }

    @Test
    @DisplayName("allows up to the maximum and refuses above it")
    void boundary() {
        counter(3L);
        assertThat(adapter.register(ORGANIZATION, 3, Duration.ofMinutes(1))).isTrue();

        counter(4L);
        assertThat(adapter.register(ORGANIZATION, 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("allows when Redis returns no count")
    void noCount() {
        counter(null);

        assertThat(adapter.register(ORGANIZATION, 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("refuses with service unavailable when Redis fails")
    void redisFails() {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenThrow(new RuntimeException("fora do ar"));

        assertThatThrownBy(() -> adapter.register(ORGANIZATION, 3, Duration.ofMinutes(1)))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Lexo IA temporariamente indisponível. Tente novamente em instantes.");
    }
}
