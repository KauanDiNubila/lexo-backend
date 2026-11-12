package app.lexo.adapter.out.lock;

import app.lexo.application.port.out.ExecutionLockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
class RedisExecutionLockAdapter implements ExecutionLockPort {

    private final StringRedisTemplate redis;

    RedisExecutionLockAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), ttl));
    }
}
