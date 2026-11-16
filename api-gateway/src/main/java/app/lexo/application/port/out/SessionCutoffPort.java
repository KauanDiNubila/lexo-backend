package app.lexo.application.port.out;

import reactor.core.publisher.Mono;

public interface SessionCutoffPort {

    Mono<Long> cutoffOf(String userId);
}
