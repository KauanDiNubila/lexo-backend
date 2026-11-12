package app.lexo.application.port.out;

import app.lexo.domain.UserId;

import java.time.Instant;

public interface RevokeUserRefreshTokensPort {

    int revokeAll(UserId userId, Instant now);
}
