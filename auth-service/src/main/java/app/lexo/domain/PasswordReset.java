package app.lexo.domain;

import java.time.Duration;
import java.time.Instant;

public class PasswordReset {

    public static final Duration VALIDITY = Duration.ofHours(1);

    private final String id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;

    private PasswordReset(String id, UserId userId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static PasswordReset issue(UserId userId, String tokenHash, Instant now) {
        return new PasswordReset(null, userId, tokenHash, now.plus(VALIDITY));
    }

    public static PasswordReset restore(String id, UserId userId, String tokenHash, Instant expiresAt) {
        return new PasswordReset(id, userId, tokenHash, expiresAt);
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt.isBefore(now);
    }

    public String id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
