package app.lexo.domain;

import java.time.Instant;

public record PortalLink(String tokenHash, Instant expiresAt) {

    public PortalLink {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("portal token hash is empty");
        }
    }

    public boolean activeAt(Instant now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }
}
