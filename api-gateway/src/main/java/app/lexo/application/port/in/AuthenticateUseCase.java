package app.lexo.application.port.in;

import app.lexo.domain.Identity;
import reactor.core.publisher.Mono;

public interface AuthenticateUseCase {

    Mono<Access> authenticate(String path, String authorization);

    sealed interface Access {

        Access PUBLIC = new Public();
        Access DENIED = new Denied();

        record Public() implements Access {
        }

        record Granted(Identity identity) implements Access {
        }

        record Denied() implements Access {
        }
    }
}
