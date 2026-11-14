package app.lexo.application.service;

import app.lexo.application.port.in.LoginCommand;
import app.lexo.application.port.in.LoginUseCase;
import app.lexo.application.port.in.SessionResponse;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.LoginAttemptsPort;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.SecretCipherPort;
import app.lexo.application.port.out.TotpPort;
import app.lexo.domain.InvalidCredentialsException;
import app.lexo.domain.InvalidTwoFactorCodeException;
import app.lexo.domain.TwoFactorRequiredException;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional
class LoginService implements LoginUseCase {

    private static final int ATTEMPTS_PER_EMAIL = 10;
    private static final int ATTEMPTS_PER_ADDRESS = 30;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoadUserPort loadUserPort;
    private final LoginAttemptsPort loginAttemptsPort;
    private final PasswordHasher passwordHasher;
    private final SecretCipherPort secretCipherPort;
    private final TotpPort totpPort;
    private final SessionIssuer sessions;

    LoginService(LoadUserPort loadUserPort, LoginAttemptsPort loginAttemptsPort, PasswordHasher passwordHasher,
                 SecretCipherPort secretCipherPort, TotpPort totpPort, SessionIssuer sessions) {
        this.loadUserPort = loadUserPort;
        this.loginAttemptsPort = loginAttemptsPort;
        this.passwordHasher = passwordHasher;
        this.secretCipherPort = secretCipherPort;
        this.totpPort = totpPort;
        this.sessions = sessions;
    }

    @Override
    public SessionResponse login(LoginCommand command) {
        String emailKey = "login:email:" + command.email().value();
        String addressKey = command.clientAddress() == null ? null : "login:ip:" + command.clientAddress();

        boolean emailWithinLimit = loginAttemptsPort.register(emailKey, ATTEMPTS_PER_EMAIL, WINDOW);
        boolean addressWithinLimit = addressKey == null
                || loginAttemptsPort.register(addressKey, ATTEMPTS_PER_ADDRESS, WINDOW);
        if (!emailWithinLimit || !addressWithinLimit) {
            throw new InvalidCredentialsException();
        }

        User user = loadUserPort.byEmail(command.email()).orElse(null);
        if (user == null) {
            passwordHasher.consumeTime(command.password());
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.requiresTwoFactor()) {
            requireValidTwoFactorCode(user, command.totpCode());
        }

        loginAttemptsPort.reset(emailKey);
        if (addressKey != null) {
            loginAttemptsPort.reset(addressKey);
        }
        return sessions.start(user);
    }

    private void requireValidTwoFactorCode(User user, String code) {
        if (code == null || code.isBlank()) {
            throw new TwoFactorRequiredException();
        }
        if (!totpPort.verify(secretCipherPort.decrypt(user.twoFactorSecret()), code)) {
            throw new InvalidTwoFactorCodeException();
        }
    }
}
