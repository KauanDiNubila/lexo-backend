package app.lexo.domain;

import app.lexo.common.security.Role;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class UserInvite {

    private static final long VALID_DAYS = 7;

    private final InviteId id;
    private final OrganizationId organizationId;
    private final String name;
    private final EmailAddress email;
    private final Role role;
    private final String token;
    private final Instant expiresAt;
    private Instant acceptedAt;
    private final Instant createdAt;

    private UserInvite(InviteId id, OrganizationId organizationId, String name, EmailAddress email, Role role,
                       String token, Instant expiresAt, Instant acceptedAt, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.createdAt = createdAt;
    }

    public static UserInvite issue(OrganizationId organizationId, String name, EmailAddress email, Role role,
                                   Instant now) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return new UserInvite(null, organizationId, name, email, role, token,
                now.plus(VALID_DAYS, ChronoUnit.DAYS), null, null);
    }

    public static UserInvite restore(InviteId id, OrganizationId organizationId, String name, EmailAddress email,
                                     Role role, String token, Instant expiresAt, Instant acceptedAt,
                                     Instant createdAt) {
        return new UserInvite(id, organizationId, name, email, role, token, expiresAt, acceptedAt, createdAt);
    }

    public InviteStatus statusAt(Instant now) {
        if (acceptedAt != null) {
            return InviteStatus.ACCEPTED;
        }
        if (expiresAt.isBefore(now)) {
            return InviteStatus.EXPIRED;
        }
        return InviteStatus.PENDING;
    }

    public void accept(Instant now) {
        this.acceptedAt = now;
    }

    public InviteId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public String name() {
        return name;
    }

    public EmailAddress email() {
        return email;
    }

    public Role role() {
        return role;
    }

    public String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
