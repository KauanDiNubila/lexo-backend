package app.lexo.application.service;

import app.lexo.application.port.in.RemoveUserCommand;
import app.lexo.application.port.in.RemoveUserUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.DeleteUserPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RemoveUserService implements RemoveUserUseCase {

    private final LoadUserPort loadUserPort;
    private final DeleteUserPort deleteUserPort;
    private final SessionTerminator sessions;
    private final PublishEventPort publishEventPort;

    RemoveUserService(LoadUserPort loadUserPort, DeleteUserPort deleteUserPort, SessionTerminator sessions,
                      PublishEventPort publishEventPort) {
        this.loadUserPort = loadUserPort;
        this.deleteUserPort = deleteUserPort;
        this.sessions = sessions;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public void remove(RemoveUserCommand command) {
        if (command.targetId().equals(command.actor().userId())) {
            throw new InvalidDataException("Você não pode remover a si mesmo");
        }
        User user = loadUserPort.inOrganization(command.organizationId(), command.targetId())
                .orElseThrow(UserNotFoundException::new);
        sessions.endAllOf(command.targetId());
        deleteUserPort.delete(command.organizationId(), command.targetId());
        publishEventPort.publish(AccountEvent.userRemoved(command.organizationId(), command.actor(), user));
    }
}
