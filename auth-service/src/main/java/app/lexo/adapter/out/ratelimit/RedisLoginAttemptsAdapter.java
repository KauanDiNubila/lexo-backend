package app.lexo.adapter.out.ratelimit;

import app.lexo.application.port.out.LoginAttemptsPort;
import app.lexo.common.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
class RedisLoginAttemptsAdapter implements LoginAttemptsPort {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginAttemptsAdapter.class);

    private static final String PREFIX = "ratelimit:";
    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count", Long.class);

    private final StringRedisTemplate redis;

    RedisLoginAttemptsAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean register(String key, int max, Duration window) {
        try {
            Long count = redis.execute(INCREMENT_WITH_TTL, List.of(PREFIX + key), String.valueOf(window.toSeconds()));
            return count == null || count <= max;
        } catch (Exception e) {
            log.error("[rate-limit] Redis is unavailable for \"{}\", refusing the request", key, e);
            throw new ServiceUnavailableException(
                    "Não foi possível validar o acesso agora. Tente novamente em instantes.");
        }
    }

    @Override
    public void reset(String key) {
        try {
            redis.delete(PREFIX + key);
        } catch (Exception e) {
            log.warn("[rate-limit] failed to clear the counter \"{}\"", key, e);
        }
    }
}
