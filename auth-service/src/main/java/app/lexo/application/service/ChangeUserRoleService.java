package app.lexo.application.service;

import app.lexo.application.port.in.ChangeUserRoleCommand;
import app.lexo.application.port.in.ChangeUserRoleUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    ChangeUserRoleService(LoadUserPort loadUserPort, SaveUserPort saveUserPort,
                          RevokeUserRefreshTokensPort revokeUserRefreshTokensPort,
                          PublishEventPort publishEventPort, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public void changeRole(ChangeUserRoleCommand command) {
        if (command.targetId().equals(command.actor().userId())) {
            throw new InvalidDataException("Você não pode alterar seu próprio papel");
        }
        User user = loadUserPort.inOrganization(command.organizationId(), command.targetId())
                .orElseThrow(UserNotFoundException::new);
        user.changeRole(command.role());
        saveUserPort.save(user);
        revokeUserRefreshTokensPort.revokeAll(command.targetId(), clock.instant());
        publishEventPort.publish(AccountEvent.roleChanged(command.organizationId(), command.actor(), user));
    }
}
