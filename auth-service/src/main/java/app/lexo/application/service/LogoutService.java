package app.lexo.application.service;

import app.lexo.application.port.in.LogoutCommand;
import app.lexo.application.port.in.LogoutUseCase;
import app.lexo.application.port.out.LoadRefreshTokenPort;
import app.lexo.application.port.out.SaveRefreshTokenPort;
import app.lexo.domain.RefreshTokenSecret;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class LogoutService implements LogoutUseCase {

    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final Clock clock;

    LogoutService(LoadRefreshTokenPort loadRefreshTokenPort, SaveRefreshTokenPort saveRefreshTokenPort,
                  Clock clock) {
        this.loadRefreshTokenPort = loadRefreshTokenPort;
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.clock = clock;
    }

    @Override
    public void logout(LogoutCommand command) {
        String raw = command.refreshToken();
        if (raw == null || raw.isBlank()) {
            return;
        }
        loadRefreshTokenPort.byHash(RefreshTokenSecret.hash(raw)).ifPresent(token -> {
            token.revoke(clock.instant());
            saveRefreshTokenPort.save(token);
        });
    }
}
