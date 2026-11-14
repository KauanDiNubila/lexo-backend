package app.lexo.adapter.out.ratelimit;

import app.lexo.application.port.out.RateLimitPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
class RedisRateLimitAdapter implements RateLimitPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitAdapter.class);

    private static final String PREFIX = "ratelimit:ia:";
    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count", Long.class);

    private final StringRedisTemplate redis;

    RedisRateLimitAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean register(OrganizationId organizationId, int max, Duration window) {
        try {
            Long count = redis.execute(INCREMENT_WITH_TTL, List.of(PREFIX + organizationId.value()),
                    String.valueOf(window.toSeconds()));
            return count == null || count <= max;
        } catch (Exception e) {
            log.error("[ia] Redis is unavailable for organization {}, refusing the call", organizationId.value(), e);
            throw new ServiceUnavailableException(
                    "Lexo IA temporariamente indisponível. Tente novamente em instantes.");
        }
    }
}
