package app.lexo.application.port.in;

import app.lexo.domain.Identity;
import reactor.core.publisher.Mono;

public interface AuthenticateUseCase {

    Mono<Access> authenticate(String path, String authorization);

    sealed interface Access {

        record Public() implements Access {
        }

        record Granted(Identity identity) implements Access {
        }

        record Denied() implements Access {
        }
    }
}
