package app.lexo.application.service;

import app.lexo.application.port.in.AcceptInviteCommand;
import app.lexo.application.port.in.AcceptInviteUseCase;
import app.lexo.application.port.out.LoadInvitePort;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.SaveInvitePort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.common.error.ConflictException;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.EmailAlreadyInUseException;
import app.lexo.domain.InviteStatus;
import app.lexo.domain.User;
import app.lexo.domain.UserInvite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class AcceptInviteService implements AcceptInviteUseCase {

    private final LoadInvitePort loadInvitePort;
    private final SaveInvitePort saveInvitePort;
    private final UserExistsPort userExistsPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    AcceptInviteService(LoadInvitePort loadInvitePort, SaveInvitePort saveInvitePort, UserExistsPort userExistsPort,
                        SaveUserPort saveUserPort, PasswordHasher passwordHasher, Clock clock) {
        this.loadInvitePort = loadInvitePort;
        this.saveInvitePort = saveInvitePort;
        this.userExistsPort = userExistsPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public void accept(AcceptInviteCommand command) {
        if (!command.password().equals(command.confirmPassword())) {
            throw new InvalidDataException("As senhas não coincidem");
        }
        Instant now = clock.instant();
        UserInvite invite = loadInvitePort.byToken(command.token())
                .orElseThrow(() -> new InvalidDataException("Convite inválido"));
        InviteStatus status = invite.statusAt(now);
        if (status == InviteStatus.ACCEPTED) {
            throw new InvalidDataException("Este convite já foi utilizado");
        }
        if (status == InviteStatus.EXPIRED) {
            throw new InvalidDataException("Este convite expirou");
        }
        if (userExistsPort.emailTaken(invite.email())) {
            throw new ConflictException("Já existe uma conta com este email");
        }

        try {
            saveUserPort.save(User.register(invite.organizationId(), invite.name(), invite.email(),
                    passwordHasher.hash(command.password()), invite.role()));
        } catch (EmailAlreadyInUseException e) {
            throw new ConflictException("Já existe uma conta com este email");
        }
        invite.accept(now);
        saveInvitePort.save(invite);
    }
}
