package app.lexo.application.service;

import app.lexo.application.port.in.InviteResponse;
import app.lexo.application.port.in.InviteUserCommand;
import app.lexo.application.port.in.InviteUserUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.InviteEmail;
import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.application.port.out.PendingInviteExistsPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveInvitePort;
import app.lexo.application.port.out.SendInviteEmailPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.common.error.ConflictException;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationNotFoundException;
import app.lexo.domain.UserInvite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class InviteUserService implements InviteUserUseCase {

    private final UserExistsPort userExistsPort;
    private final PendingInviteExistsPort pendingInviteExistsPort;
    private final SaveInvitePort saveInvitePort;
    private final LoadOrganizationPort loadOrganizationPort;
    private final SendInviteEmailPort sendInviteEmailPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    InviteUserService(UserExistsPort userExistsPort, PendingInviteExistsPort pendingInviteExistsPort,
                      SaveInvitePort saveInvitePort, LoadOrganizationPort loadOrganizationPort,
                      SendInviteEmailPort sendInviteEmailPort, PublishEventPort publishEventPort, Clock clock) {
        this.userExistsPort = userExistsPort;
        this.pendingInviteExistsPort = pendingInviteExistsPort;
        this.saveInvitePort = saveInvitePort;
        this.loadOrganizationPort = loadOrganizationPort;
        this.sendInviteEmailPort = sendInviteEmailPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public InviteResponse invite(InviteUserCommand command) {
        Instant now = clock.instant();
        if (userExistsPort.emailTaken(command.email())) {
            throw new ConflictException("Já existe um usuário com este email");
        }
        if (pendingInviteExistsPort.exists(command.email(), command.organizationId(), now)) {
            throw new ConflictException("Já existe um convite pendente para este email");
        }

        UserInvite invite = saveInvitePort.save(UserInvite.issue(command.organizationId(), command.name(),
                command.email(), command.role(), now));

        String organizationName = loadOrganizationPort.byId(command.organizationId())
                .map(Organization::name).orElseThrow(OrganizationNotFoundException::new);
        sendInviteEmailPort.send(new InviteEmail(command.email(), command.name(), organizationName,
                invite.token()));
        publishEventPort.publish(AccountEvent.userInvited(command.organizationId(), command.actor(),
                command.name(), command.email(), command.role()));
        return InviteResponse.of(invite);
    }
}
