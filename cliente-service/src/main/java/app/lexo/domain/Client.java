package app.lexo.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Client {

    private final ClientId id;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private ClientData data;
    private PortalLink portalLink;

    private Client(ClientId id, OrganizationId organizationId, ClientData data, PortalLink portalLink,
                    Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.data = data;
        this.portalLink = portalLink;
        this.createdAt = createdAt;
    }

    public static Client register(OrganizationId organizationId, ClientData data) {
        return new Client(null, organizationId, data, null, null);
    }

    public static Client restore(ClientId id, OrganizationId organizationId, ClientData data,
                                       PortalLink portalLink, Instant createdAt) {
        return new Client(id, organizationId, data, portalLink, createdAt);
    }

    public void update(ClientData newData) {
        this.data = newData;
    }

    public PortalLink generatePortalLink(String tokenHash, Instant now, long validityDays) {
        this.portalLink = new PortalLink(tokenHash, now.plus(validityDays, ChronoUnit.DAYS));
        return portalLink;
    }

    public void revokePortalLink() {
        this.portalLink = null;
    }

    public boolean portalActiveAt(Instant now) {
        return portalLink != null && portalLink.activeAt(now);
    }

    public ClientId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public ClientData data() {
        return data;
    }

    public String name() {
        return data.name();
    }

    public PortalLink portalLink() {
        return portalLink;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
