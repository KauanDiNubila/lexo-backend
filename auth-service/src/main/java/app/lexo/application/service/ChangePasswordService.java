package app.lexo.application.service;

import app.lexo.application.port.in.ChangePasswordCommand;
import app.lexo.application.port.in.ChangePasswordUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.LoginAttemptsPort;
import app.lexo.application.port.out.PasswordChangedEmail;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.SendPasswordEmailPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.error.TooManyRequestsException;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional
class ChangePasswordService implements ChangePasswordUseCase {

    private static final int ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoginAttemptsPort loginAttemptsPort;
    private final PasswordHasher passwordHasher;
    private final SessionTerminator sessions;
    private final PublishEventPort publishEventPort;
    private final SendPasswordEmailPort sendPasswordEmailPort;

    ChangePasswordService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, LoginAttemptsPort loginAttemptsPort,
                          PasswordHasher passwordHasher, SessionTerminator sessions,
                          PublishEventPort publishEventPort, SendPasswordEmailPort sendPasswordEmailPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loginAttemptsPort = loginAttemptsPort;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.publishEventPort = publishEventPort;
        this.sendPasswordEmailPort = sendPasswordEmailPort;
    }

    @Override
    public void change(ChangePasswordCommand command) {
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        String attemptsKey = "password-change:" + command.actor().userId().value();
        if (!loginAttemptsPort.register(attemptsKey, ATTEMPTS, WINDOW)) {
            throw new TooManyRequestsException("Muitas tentativas. Tente novamente em alguns minutos.");
        }
        User user = loadUserPort.inOrganization(command.organizationId(), command.actor().userId())
                .orElseThrow(UserNotFoundException::new);
        if (!passwordHasher.matches(command.currentPassword(), user.passwordHash())) {
            throw new InvalidDataException("Senha atual incorreta");
        }
        if (command.newPassword().equals(command.currentPassword())) {
            throw new InvalidDataException("A nova senha deve ser diferente da atual");
        }

        user.changePassword(passwordHasher.hash(command.newPassword()));
        saveUserPort.save(user);
        loginAttemptsPort.reset(attemptsKey);
        sessions.endAllOf(user.id());
        publishEventPort.publish(AccountEvent.passwordChanged(command.organizationId(), command.actor()));
        sendPasswordEmailPort.sendChangedNotice(new PasswordChangedEmail(user.email(), user.name()));
    }
}
