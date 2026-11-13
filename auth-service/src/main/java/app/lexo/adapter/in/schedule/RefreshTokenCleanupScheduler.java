package app.lexo.adapter.in.schedule;

import app.lexo.application.port.in.PurgeRefreshTokensUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final PurgeRefreshTokensUseCase purgeRefreshTokens;

    RefreshTokenCleanupScheduler(PurgeRefreshTokensUseCase purgeRefreshTokens) {
        this.purgeRefreshTokens = purgeRefreshTokens;
    }

    @Scheduled(cron = "0 30 3 * * *")
    void run() {
        int purged = purgeRefreshTokens.purge();
        log.info("[refresh] {} expired refresh token(s) removed", purged);
    }
}
