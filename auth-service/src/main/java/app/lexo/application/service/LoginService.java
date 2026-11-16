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
import java.util.List;

@Service
@Transactional
class LoginService implements LoginUseCase {

    private static final int ATTEMPTS_PER_EMAIL_AND_ADDRESS = 10;
    private static final int ATTEMPTS_PER_EMAIL = 50;
    private static final int ATTEMPTS_PER_EMAIL_WITHOUT_ADDRESS = 10;
    private static final int ATTEMPTS_PER_ADDRESS = 30;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Limit(String key, int max) {
    }

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
        List<Limit> limits = limitsFor(command.email().value(), command.clientAddress());

        boolean withinLimits = true;
        for (Limit limit : limits) {
            withinLimits &= loginAttemptsPort.register(limit.key(), limit.max(), WINDOW);
        }
        if (!withinLimits) {
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

        limits.forEach(limit -> loginAttemptsPort.reset(limit.key()));
        return sessions.start(user);
    }

    private List<Limit> limitsFor(String email, String address) {
        String emailKey = "login:email:" + email;
        if (address == null) {
            return List.of(new Limit(emailKey, ATTEMPTS_PER_EMAIL_WITHOUT_ADDRESS));
        }
        return List.of(
                new Limit("login:email-ip:" + email + "|" + address, ATTEMPTS_PER_EMAIL_AND_ADDRESS),
                new Limit(emailKey, ATTEMPTS_PER_EMAIL),
                new Limit("login:ip:" + address, ATTEMPTS_PER_ADDRESS));
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
