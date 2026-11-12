package app.lexo.application.service;

import app.lexo.application.port.in.RemoveUserCommand;
import app.lexo.application.port.in.RemoveUserUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.DeleteUserPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class RemoveUserService implements RemoveUserUseCase {

    private final LoadUserPort loadUserPort;
    private final DeleteUserPort deleteUserPort;
    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    RemoveUserService(LoadUserPort loadUserPort, DeleteUserPort deleteUserPort,
                      RevokeUserRefreshTokensPort revokeUserRefreshTokensPort, PublishEventPort publishEventPort,
                      Clock clock) {
        this.loadUserPort = loadUserPort;
        this.deleteUserPort = deleteUserPort;
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public void remove(RemoveUserCommand command) {
        if (command.targetId().equals(command.actor().userId())) {
            throw new InvalidDataException("Você não pode remover a si mesmo");
        }
        User user = loadUserPort.inOrganization(command.organizationId(), command.targetId())
                .orElseThrow(UserNotFoundException::new);
        revokeUserRefreshTokensPort.revokeAll(command.targetId(), clock.instant());
        deleteUserPort.delete(command.targetId());
        publishEventPort.publish(AccountEvent.userRemoved(command.organizationId(), command.actor(), user));
    }
}
