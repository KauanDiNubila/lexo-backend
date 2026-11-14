package app.lexo.application.service;

import app.lexo.application.port.in.RevokeInviteCommand;
import app.lexo.application.port.in.RevokeInviteUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.DeleteInvitePort;
import app.lexo.application.port.out.LoadInvitePort;
import app.lexo.application.port.out.PublishEventPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RevokeInviteService implements RevokeInviteUseCase {

    private final LoadInvitePort loadInvitePort;
    private final DeleteInvitePort deleteInvitePort;
    private final PublishEventPort publishEventPort;

    RevokeInviteService(LoadInvitePort loadInvitePort, DeleteInvitePort deleteInvitePort,
                        PublishEventPort publishEventPort) {
        this.loadInvitePort = loadInvitePort;
        this.deleteInvitePort = deleteInvitePort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public void revoke(RevokeInviteCommand command) {
        loadInvitePort.inOrganization(command.organizationId(), command.inviteId()).ifPresent(invite -> {
            deleteInvitePort.delete(command.organizationId(), command.inviteId());
            publishEventPort.publish(AccountEvent.inviteRevoked(command.organizationId(), command.actor(), invite));
        });
    }
}
