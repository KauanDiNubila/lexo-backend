package app.lexo.application.port.out;

import app.lexo.domain.PasswordReset;

import java.util.Optional;

public interface LoadPasswordResetPort {

    Optional<PasswordReset> byTokenHash(String tokenHash);
}
