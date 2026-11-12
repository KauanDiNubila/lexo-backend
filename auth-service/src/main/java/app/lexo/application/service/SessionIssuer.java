package app.lexo.application.service;

import app.lexo.application.port.in.SessionResponse;
import app.lexo.application.port.in.UserInfo;
import app.lexo.application.port.out.IssueAccessTokenPort;
import app.lexo.application.port.out.SaveRefreshTokenPort;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.RefreshTokenSecret;
import app.lexo.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Component
class SessionIssuer {

    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final IssueAccessTokenPort issueAccessTokenPort;
    private final Clock clock;
    private final long refreshValidityDays;

    SessionIssuer(SaveRefreshTokenPort saveRefreshTokenPort, IssueAccessTokenPort issueAccessTokenPort,
                  Clock clock, @Value("${lexo.refresh-expiration-days}") long refreshValidityDays) {
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.issueAccessTokenPort = issueAccessTokenPort;
        this.clock = clock;
        this.refreshValidityDays = refreshValidityDays;
    }

    SessionResponse start(User user) {
        String rawRefreshToken = RefreshTokenSecret.generate();
        saveRefreshTokenPort.save(RefreshToken.issue(user.id(), RefreshTokenSecret.hash(rawRefreshToken),
                clock.instant().plus(refreshValidityDays, ChronoUnit.DAYS)));
        return new SessionResponse(issueAccessTokenPort.issue(user), UserInfo.of(user), rawRefreshToken);
    }
}
