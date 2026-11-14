package app.lexo.application.service;

import app.lexo.application.port.in.ChangeUserRoleCommand;
import app.lexo.application.port.in.ChangeUserRoleUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.RevokeAccessTokensPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final RevokeAccessTokensPort revokeAccessTokensPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    ChangeUserRoleService(LoadUserPort loadUserPort, SaveUserPort saveUserPort,
                          RevokeUserRefreshTokensPort revokeUserRefreshTokensPort,
                          RevokeAccessTokensPort revokeAccessTokensPort,
                          PublishEventPort publishEventPort, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.revokeAccessTokensPort = revokeAccessTokensPort;
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
        Instant now = clock.instant();
        revokeUserRefreshTokensPort.revokeAll(command.targetId(), now);
        revokeAccessTokensPort.revokeIssuedUpTo(command.targetId(), now);
        publishEventPort.publish(AccountEvent.roleChanged(command.organizationId(), command.actor(), user));
    }
}
