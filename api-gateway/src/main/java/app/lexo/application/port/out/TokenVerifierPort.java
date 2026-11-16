package app.lexo.application.port.out;

import app.lexo.domain.Session;

import java.util.Optional;

public interface TokenVerifierPort {

    Optional<Session> verify(String token);
}
