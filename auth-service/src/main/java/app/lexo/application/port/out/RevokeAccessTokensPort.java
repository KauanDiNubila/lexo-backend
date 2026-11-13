package app.lexo.application.port.out;

import app.lexo.domain.UserId;

import java.time.Instant;

public interface RevokeAccessTokensPort {

    void revokeIssuedUpTo(UserId userId, Instant now);
}
