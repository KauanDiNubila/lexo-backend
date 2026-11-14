package app.lexo.domain;

import java.time.Instant;

public class RefreshToken {

    private final String id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshToken(String id, UserId userId, String tokenHash, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UserId userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(null, userId, tokenHash, expiresAt, null);
    }

    public static RefreshToken restore(String id, UserId userId, String tokenHash, Instant expiresAt,
                                       Instant revokedAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt);
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
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

    public Instant revokedAt() {
        return revokedAt;
    }
}
