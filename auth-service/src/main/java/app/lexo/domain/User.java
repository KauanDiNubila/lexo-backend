package app.lexo.domain;

import app.lexo.common.security.Role;

import java.time.Instant;
import java.util.Optional;

public class User {

    private final UserId id;
    private final OrganizationId organizationId;
    private String name;
    private final EmailAddress email;
    private final String passwordHash;
    private Role role;
    private String twoFactorSecret;
    private boolean twoFactorEnabled;
    private String pendingTwoFactorSecret;
    private final Instant createdAt;

    private User(UserId id, OrganizationId organizationId, String name, EmailAddress email, String passwordHash,
                 Role role, String twoFactorSecret, boolean twoFactorEnabled, String pendingTwoFactorSecret,
                 Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.twoFactorSecret = twoFactorSecret;
        this.twoFactorEnabled = twoFactorEnabled;
        this.pendingTwoFactorSecret = pendingTwoFactorSecret;
        this.createdAt = createdAt;
    }

    public static User register(OrganizationId organizationId, String name, EmailAddress email,
                                String passwordHash, Role role) {
        return new User(null, organizationId, name, email, passwordHash, role, null, false, null, null);
    }

    public static User restore(UserId id, OrganizationId organizationId, String name, EmailAddress email,
                               String passwordHash, Role role, String twoFactorSecret, boolean twoFactorEnabled,
                               String pendingTwoFactorSecret, Instant createdAt) {
        return new User(id, organizationId, name, email, passwordHash, role, twoFactorSecret, twoFactorEnabled,
                pendingTwoFactorSecret, createdAt);
    }

    public void rename(String newName) {
        this.name = newName.trim();
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public boolean requiresTwoFactor() {
        return twoFactorEnabled && twoFactorSecret != null;
    }

    public boolean hasTwoFactorSecret() {
        return twoFactorSecret != null;
    }

    public void beginTwoFactorSetup(String encryptedSecret) {
        this.pendingTwoFactorSecret = encryptedSecret;
    }

    public Optional<String> pendingTwoFactorSecret() {
        return Optional.ofNullable(pendingTwoFactorSecret);
    }

    public void confirmTwoFactorSetup() {
        if (pendingTwoFactorSecret == null) {
            throw new IllegalStateException("There is no two factor setup to confirm");
        }
        this.twoFactorSecret = pendingTwoFactorSecret;
        this.twoFactorEnabled = true;
        this.pendingTwoFactorSecret = null;
    }

    public void disableTwoFactor() {
        this.twoFactorSecret = null;
        this.twoFactorEnabled = false;
        this.pendingTwoFactorSecret = null;
    }

    public UserId id() {
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

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public String twoFactorSecret() {
        return twoFactorSecret;
    }

    public boolean twoFactorEnabled() {
        return twoFactorEnabled;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
