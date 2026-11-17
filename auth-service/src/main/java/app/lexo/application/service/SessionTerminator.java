package app.lexo.application.service;

import app.lexo.application.port.out.RevokeAccessTokensPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class SessionTerminator {

    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final RevokeAccessTokensPort revokeAccessTokensPort;
    private final Clock clock;

    SessionTerminator(RevokeUserRefreshTokensPort revokeUserRefreshTokensPort,
                      RevokeAccessTokensPort revokeAccessTokensPort, Clock clock) {
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.revokeAccessTokensPort = revokeAccessTokensPort;
        this.clock = clock;
    }

    void endAllOf(UserId userId) {
        Instant now = clock.instant();
        revokeUserRefreshTokensPort.revokeAll(userId, now);
        revokeAccessTokensPort.revokeIssuedUpTo(userId, now);
    }
}
