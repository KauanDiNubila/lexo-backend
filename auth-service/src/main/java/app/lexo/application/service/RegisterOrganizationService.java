package app.lexo.application.service;

import app.lexo.application.port.in.RegisterOrganizationCommand;
import app.lexo.application.port.in.RegisterOrganizationUseCase;
import app.lexo.application.port.in.SessionResponse;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.SaveOrganizationPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAlreadyInUseException;
import app.lexo.domain.Organization;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class RegisterOrganizationService implements RegisterOrganizationUseCase {

    private final UserExistsPort userExistsPort;
    private final SaveOrganizationPort saveOrganizationPort;
    private final SaveUserPort saveUserPort;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    RegisterOrganizationService(UserExistsPort userExistsPort, SaveOrganizationPort saveOrganizationPort,
                                SaveUserPort saveUserPort, PasswordHasher passwordHasher, SessionIssuer sessions,
                                Clock clock) {
        this.userExistsPort = userExistsPort;
        this.saveOrganizationPort = saveOrganizationPort;
        this.saveUserPort = saveUserPort;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public SessionResponse register(RegisterOrganizationCommand command) {
        if (!command.password().equals(command.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        if (userExistsPort.emailTaken(command.email())) {
            throw new EmailAlreadyInUseException();
        }
        Organization organization = saveOrganizationPort.save(
                Organization.register(command.organizationName(), clock.instant()));
        User admin = saveUserPort.save(User.register(organization.id(), command.name(), command.email(),
                passwordHasher.hash(command.password()), Role.ADMIN));
        return sessions.start(admin);
    }
}
