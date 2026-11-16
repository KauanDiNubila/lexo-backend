package app.lexo.adapter.out.redis;

import app.lexo.application.port.out.SessionCutoffPort;
import app.lexo.common.signing.SessionCutoff;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class RedisSessionCutoffAdapter implements SessionCutoffPort {

    private final ReactiveStringRedisTemplate redis;

    RedisSessionCutoffAdapter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Long> cutoffOf(String userId) {
        return redis.opsForValue().get(SessionCutoff.key(userId)).map(Long::parseLong);
    }
}
