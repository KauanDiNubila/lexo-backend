package app.lexo.application.service;

import app.lexo.application.port.in.CheckRateLimitUseCase;
import app.lexo.application.port.out.RateLimitPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class CheckRateLimitService implements CheckRateLimitUseCase {

    private final RateLimitPort rateLimit;

    CheckRateLimitService(RateLimitPort rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public Mono<Boolean> isAllowed(String client) {
        return rateLimit.isAllowed(client);
    }
}
