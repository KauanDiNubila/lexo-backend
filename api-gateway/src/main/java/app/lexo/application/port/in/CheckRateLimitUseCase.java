package app.lexo.application.port.in;

import reactor.core.publisher.Mono;

public interface CheckRateLimitUseCase {

    Mono<Boolean> isAllowed(String client);
}
