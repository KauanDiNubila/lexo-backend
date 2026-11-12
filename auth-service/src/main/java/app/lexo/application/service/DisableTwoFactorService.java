package app.lexo.application.service;

import app.lexo.application.port.in.DisableTwoFactorCommand;
import app.lexo.application.port.in.DisableTwoFactorUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.SecretCipherPort;
import app.lexo.application.port.out.TotpPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.UnknownSessionUserException;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DisableTwoFactorService implements DisableTwoFactorUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final SecretCipherPort secretCipherPort;
    private final TotpPort totpPort;
    private final PublishEventPort publishEventPort;

    DisableTwoFactorService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, SecretCipherPort secretCipherPort,
                            TotpPort totpPort, PublishEventPort publishEventPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.secretCipherPort = secretCipherPort;
        this.totpPort = totpPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public void disable(DisableTwoFactorCommand command) {
        User user = loadUserPort.inOrganization(command.organizationId(), command.actor().userId())
                .orElseThrow(UnknownSessionUserException::new);
        if (!user.hasTwoFactorSecret()) {
            throw new InvalidDataException("2FA não está ativado.");
        }
        if (!totpPort.verify(secretCipherPort.decrypt(user.twoFactorSecret()), command.code())) {
            throw new InvalidDataException("Código inválido.");
        }
        user.disableTwoFactor();
        saveUserPort.save(user);
        publishEventPort.publish(AccountEvent.twoFactorDisabled(command.organizationId(), command.actor()));
    }
}
