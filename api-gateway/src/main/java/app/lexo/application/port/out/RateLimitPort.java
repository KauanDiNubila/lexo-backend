package app.lexo.application.port.out;

import reactor.core.publisher.Mono;

public interface RateLimitPort {

    Mono<Boolean> isAllowed(String client);
}
