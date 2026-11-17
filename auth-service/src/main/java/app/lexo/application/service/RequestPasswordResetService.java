package app.lexo.application.service;

import app.lexo.application.port.in.RequestPasswordResetCommand;
import app.lexo.application.port.in.RequestPasswordResetUseCase;
import app.lexo.application.port.out.DeletePasswordResetsPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.LoginAttemptsPort;
import app.lexo.application.port.out.PasswordResetEmail;
import app.lexo.application.port.out.SavePasswordResetPort;
import app.lexo.application.port.out.SendPasswordEmailPort;
import app.lexo.domain.PasswordReset;
import app.lexo.domain.TokenSecret;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Transactional
class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private static final int REQUESTS_PER_EMAIL = 3;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final LoadUserPort loadUserPort;
    private final LoginAttemptsPort loginAttemptsPort;
    private final DeletePasswordResetsPort deletePasswordResetsPort;
    private final SavePasswordResetPort savePasswordResetPort;
    private final SendPasswordEmailPort sendPasswordEmailPort;
    private final Clock clock;

    RequestPasswordResetService(LoadUserPort loadUserPort, LoginAttemptsPort loginAttemptsPort,
                                DeletePasswordResetsPort deletePasswordResetsPort,
                                SavePasswordResetPort savePasswordResetPort,
                                SendPasswordEmailPort sendPasswordEmailPort, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.loginAttemptsPort = loginAttemptsPort;
        this.deletePasswordResetsPort = deletePasswordResetsPort;
        this.savePasswordResetPort = savePasswordResetPort;
        this.sendPasswordEmailPort = sendPasswordEmailPort;
        this.clock = clock;
    }

    @Override
    public void request(RequestPasswordResetCommand command) {
        if (!loginAttemptsPort.register("password-reset:email:" + command.email().value(), REQUESTS_PER_EMAIL,
                WINDOW)) {
            return;
        }
        loadUserPort.byEmail(command.email()).ifPresent(this::issueFor);
    }

    private void issueFor(User user) {
        Instant now = clock.instant();
        String rawToken = TokenSecret.generate();
        deletePasswordResetsPort.expiredOf(user.id(), now);
        savePasswordResetPort.save(PasswordReset.issue(user.id(), TokenSecret.hash(rawToken), now));
        sendPasswordEmailPort.sendResetLink(new PasswordResetEmail(user.email(), user.name(), rawToken));
    }
}
