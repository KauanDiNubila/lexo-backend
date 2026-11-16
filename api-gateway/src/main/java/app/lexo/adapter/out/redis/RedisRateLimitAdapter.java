package app.lexo.adapter.out.redis;

import app.lexo.application.port.out.RateLimitPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class RedisRateLimitAdapter implements RateLimitPort {

    private static final String GLOBAL_ROUTE = "gateway-global";

    private final RedisRateLimiter limiter;

    RedisRateLimitAdapter(RedisRateLimiter limiter,
                          @Value("${lexo.rate-limit.replenish-rate}") int replenishRate,
                          @Value("${lexo.rate-limit.burst-capacity}") int burstCapacity,
                          @Value("${lexo.rate-limit.requested-tokens}") int requestedTokens) {
        this.limiter = limiter;
        limiter.getConfig().put(GLOBAL_ROUTE, new RedisRateLimiter.Config()
                .setReplenishRate(replenishRate)
                .setBurstCapacity(burstCapacity)
                .setRequestedTokens(requestedTokens));
    }

    @Override
    public Mono<Boolean> isAllowed(String client) {
        return limiter.isAllowed(GLOBAL_ROUTE, client).map(response -> response.isAllowed());
    }
}
