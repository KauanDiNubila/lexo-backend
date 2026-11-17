package app.lexo.application.service;

import app.lexo.application.port.in.ResetPasswordCommand;
import app.lexo.application.port.in.ResetPasswordUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.DeletePasswordResetsPort;
import app.lexo.application.port.out.LoadPasswordResetPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PasswordChangedEmail;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.SendPasswordEmailPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.TokenSecret;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class ResetPasswordService implements ResetPasswordUseCase {

    private static final String INVALID_LINK = "Link inválido ou expirado";

    private final LoadPasswordResetPort loadPasswordResetPort;
    private final DeletePasswordResetsPort deletePasswordResetsPort;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;
    private final SessionTerminator sessions;
    private final PublishEventPort publishEventPort;
    private final SendPasswordEmailPort sendPasswordEmailPort;
    private final Clock clock;

    ResetPasswordService(LoadPasswordResetPort loadPasswordResetPort,
                         DeletePasswordResetsPort deletePasswordResetsPort, LoadUserPort loadUserPort,
                         SaveUserPort saveUserPort, PasswordHasher passwordHasher, SessionTerminator sessions,
                         PublishEventPort publishEventPort, SendPasswordEmailPort sendPasswordEmailPort,
                         Clock clock) {
        this.loadPasswordResetPort = loadPasswordResetPort;
        this.deletePasswordResetsPort = deletePasswordResetsPort;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.publishEventPort = publishEventPort;
        this.sendPasswordEmailPort = sendPasswordEmailPort;
        this.clock = clock;
    }

    @Override
    public void reset(ResetPasswordCommand command) {
        if (!command.password().equals(command.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        User user = loadPasswordResetPort.byTokenHash(TokenSecret.hash(command.token()))
                .filter(reset -> !reset.isExpiredAt(clock.instant()))
                .flatMap(reset -> loadUserPort.byId(reset.userId()))
                .orElseThrow(() -> new InvalidDataException(INVALID_LINK));

        user.changePassword(passwordHasher.hash(command.password()));
        saveUserPort.save(user);
        deletePasswordResetsPort.ofUser(user.id());
        sessions.endAllOf(user.id());
        publishEventPort.publish(AccountEvent.passwordReset(user));
        sendPasswordEmailPort.sendChangedNotice(new PasswordChangedEmail(user.email(), user.name()));
    }
}
