package app.lexo.application.service;

import app.lexo.application.port.in.StartTwoFactorCommand;
import app.lexo.application.port.in.StartTwoFactorUseCase;
import app.lexo.application.port.in.TwoFactorSetup;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.SecretCipherPort;
import app.lexo.application.port.out.TotpPort;
import app.lexo.domain.UnknownSessionUserException;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class StartTwoFactorService implements StartTwoFactorUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final SecretCipherPort secretCipherPort;
    private final TotpPort totpPort;

    StartTwoFactorService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, SecretCipherPort secretCipherPort,
                          TotpPort totpPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.secretCipherPort = secretCipherPort;
        this.totpPort = totpPort;
    }

    @Override
    public TwoFactorSetup start(StartTwoFactorCommand command) {
        User user = loadUserPort.inOrganization(command.organizationId(), command.userId())
                .orElseThrow(UnknownSessionUserException::new);
        String secret = totpPort.generateSecret();
        user.beginTwoFactorSetup(secretCipherPort.encrypt(secret));
        saveUserPort.save(user);
        return new TwoFactorSetup(secret, totpPort.otpauthUri(secret, user.email()));
    }
}
