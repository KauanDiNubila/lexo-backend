package app.lexo.application.service;

import app.lexo.application.port.in.ProfileInfo;
import app.lexo.application.port.in.RenameProfileCommand;
import app.lexo.application.port.in.RenameProfileUseCase;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RenameProfileService implements RenameProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    RenameProfileService(LoadUserPort loadUserPort, SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public ProfileInfo rename(RenameProfileCommand command) {
        User user = loadUserPort.inOrganization(command.organizationId(), command.userId())
                .orElseThrow(UserNotFoundException::new);
        user.rename(command.name());
        return ProfileInfo.of(saveUserPort.save(user));
    }
}
