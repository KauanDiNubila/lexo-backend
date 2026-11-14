package app.lexo.application.service;

import app.lexo.application.port.in.PurgeRefreshTokensUseCase;
import app.lexo.application.port.out.DeleteExpiredRefreshTokensPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@Transactional
class PurgeRefreshTokensService implements PurgeRefreshTokensUseCase {

    private static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(7);

    private final DeleteExpiredRefreshTokensPort deleteExpiredRefreshTokensPort;
    private final Clock clock;

    PurgeRefreshTokensService(DeleteExpiredRefreshTokensPort deleteExpiredRefreshTokensPort, Clock clock) {
        this.deleteExpiredRefreshTokensPort = deleteExpiredRefreshTokensPort;
        this.clock = clock;
    }

    @Override
    public int purge() {
        return deleteExpiredRefreshTokensPort.deleteExpiredBefore(clock.instant().minus(RETENTION_AFTER_EXPIRY));
    }
}
