package app.lexo.application.service;

import app.lexo.application.port.in.ConfirmTwoFactorCommand;
import app.lexo.application.port.in.ConfirmTwoFactorUseCase;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.RevokeAccessTokensPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.SecretCipherPort;
import app.lexo.application.port.out.TotpPort;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.UnknownSessionUserException;
import app.lexo.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class ConfirmTwoFactorService implements ConfirmTwoFactorUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final SecretCipherPort secretCipherPort;
    private final TotpPort totpPort;
    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final RevokeAccessTokensPort revokeAccessTokensPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    ConfirmTwoFactorService(LoadUserPort loadUserPort, SaveUserPort saveUserPort, SecretCipherPort secretCipherPort,
                            TotpPort totpPort, RevokeUserRefreshTokensPort revokeUserRefreshTokensPort,
                           RevokeAccessTokensPort revokeAccessTokensPort, PublishEventPort publishEventPort, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.secretCipherPort = secretCipherPort;
        this.totpPort = totpPort;
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.revokeAccessTokensPort = revokeAccessTokensPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public void confirm(ConfirmTwoFactorCommand command) {
        User user = loadUserPort.inOrganization(command.organizationId(), command.actor().userId())
                .orElseThrow(UnknownSessionUserException::new);
        String pending = user.pendingTwoFactorSecret()
                .orElseThrow(() -> new InvalidDataException("Sessão expirada. Reinicie o processo."));
        if (!totpPort.verify(secretCipherPort.decrypt(pending), command.code())) {
            throw new InvalidDataException("Código inválido. Tente novamente.");
        }
        user.confirmTwoFactorSetup();
        saveUserPort.save(user);
        endSessionsOf(user);
        publishEventPort.publish(AccountEvent.twoFactorEnabled(command.organizationId(), command.actor()));
    }

    private void endSessionsOf(User user) {
        Instant now = clock.instant();
        revokeUserRefreshTokensPort.revokeAll(user.id(), now);
        revokeAccessTokensPort.revokeIssuedUpTo(user.id(), now);
    }
}
