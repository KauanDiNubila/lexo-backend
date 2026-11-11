package app.lexo.domain;

import java.time.Instant;

public record PortalLink(String token, Instant expiresAt) {

    public PortalLink {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("portal token is empty");
        }
    }

    public boolean activeAt(Instant now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }
}
