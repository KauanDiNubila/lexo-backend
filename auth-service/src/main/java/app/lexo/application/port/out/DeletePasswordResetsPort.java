package app.lexo.application.port.out;

import app.lexo.domain.UserId;

import java.time.Instant;

public interface DeletePasswordResetsPort {

    void ofUser(UserId userId);

    void expiredOf(UserId userId, Instant now);
}
