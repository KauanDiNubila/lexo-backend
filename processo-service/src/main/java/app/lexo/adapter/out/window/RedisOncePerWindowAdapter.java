package app.lexo.adapter.out.window;

import app.lexo.application.port.out.OncePerWindowPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
class RedisOncePerWindowAdapter implements OncePerWindowPort {

    private final StringRedisTemplate redis;

    RedisOncePerWindowAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryClaim(String key, Duration window) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), window));
    }
}
