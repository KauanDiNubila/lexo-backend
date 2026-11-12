package app.lexo.application.service;

import app.lexo.application.port.in.RefreshSessionCommand;
import app.lexo.application.port.in.RefreshSessionUseCase;
import app.lexo.application.port.in.SessionResponse;
import app.lexo.application.port.out.LoadRefreshTokenPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveRefreshTokenPort;
import app.lexo.domain.ExpiredRefreshTokenException;
import app.lexo.domain.InvalidRefreshTokenException;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.RefreshTokenSecret;
import app.lexo.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional(noRollbackFor = {InvalidRefreshTokenException.class, ExpiredRefreshTokenException.class})
class RefreshSessionService implements RefreshSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshSessionService.class);

    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final RevokeUserRefreshTokensPort revokeUserRefreshTokensPort;
    private final LoadUserPort loadUserPort;
    private final SessionIssuer sessions;
    private final Clock clock;

    RefreshSessionService(LoadRefreshTokenPort loadRefreshTokenPort, SaveRefreshTokenPort saveRefreshTokenPort,
                          RevokeUserRefreshTokensPort revokeUserRefreshTokensPort, LoadUserPort loadUserPort,
                          SessionIssuer sessions, Clock clock) {
        this.loadRefreshTokenPort = loadRefreshTokenPort;
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.revokeUserRefreshTokensPort = revokeUserRefreshTokensPort;
        this.loadUserPort = loadUserPort;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public SessionResponse refresh(RefreshSessionCommand command) {
        String raw = command.refreshToken();
        if (raw == null || raw.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken current = loadRefreshTokenPort.byHash(RefreshTokenSecret.hash(raw))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            int revoked = revokeUserRefreshTokensPort.revokeAll(current.userId(), now);
            log.warn("[refresh] revoked token presented again for user {}, {} session(s) revoked on suspicion "
                    + "of theft", current.userId().value(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now)) {
            throw new ExpiredRefreshTokenException();
        }

        current.revoke(now);
        saveRefreshTokenPort.save(current);
        User user = loadUserPort.byId(current.userId()).orElseThrow(InvalidRefreshTokenException::new);
        return sessions.start(user);
    }
}
