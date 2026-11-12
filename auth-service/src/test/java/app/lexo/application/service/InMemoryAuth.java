package app.lexo.application.service;

import app.lexo.application.port.out.CountUsersPort;
import app.lexo.application.port.out.DeleteInvitePort;
import app.lexo.application.port.out.DeleteUserPort;
import app.lexo.application.port.out.ListPendingInvitesPort;
import app.lexo.application.port.out.ListUsersPort;
import app.lexo.application.port.out.LoadInvitePort;
import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.application.port.out.LoadRefreshTokenPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.PendingInviteExistsPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveInvitePort;
import app.lexo.application.port.out.SaveOrganizationPort;
import app.lexo.application.port.out.SaveRefreshTokenPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.InviteId;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.User;
import app.lexo.domain.UserId;
import app.lexo.domain.UserInvite;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryAuth implements SaveUserPort, LoadUserPort, UserExistsPort, ListUsersPort, CountUsersPort,
        DeleteUserPort, SaveOrganizationPort, LoadOrganizationPort, SaveRefreshTokenPort, LoadRefreshTokenPort,
        RevokeUserRefreshTokensPort, SaveInvitePort, LoadInvitePort, ListPendingInvitesPort,
        PendingInviteExistsPort, DeleteInvitePort {

    static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    final Map<String, User> users = new LinkedHashMap<>();
    final Map<String, Organization> organizations = new LinkedHashMap<>();
    final Map<String, RefreshToken> tokens = new LinkedHashMap<>();
    final Map<String, UserInvite> invites = new LinkedHashMap<>();
    private int sequence;

    private String nextId(String prefix) {
        return prefix + "-" + (++sequence);
    }

    @Override
    public User save(User user) {
        UserId id = user.id() == null ? new UserId(nextId("user")) : user.id();
        User stored = copy(id, user);
        users.put(id.value(), stored);
        return copy(id, stored);
    }

    private User copy(UserId id, User user) {
        return User.restore(id, user.organizationId(), user.name(), user.email(), user.passwordHash(), user.role(),
                user.twoFactorSecret(), user.twoFactorEnabled(), user.pendingTwoFactorSecret().orElse(null),
                user.createdAt() == null ? CREATED : user.createdAt());
    }

    @Override
    public Optional<User> byEmail(EmailAddress email) {
        return users.values().stream().filter(user -> user.email().equals(email)).findFirst()
                .map(user -> copy(user.id(), user));
    }

    @Override
    public Optional<User> byId(UserId id) {
        return Optional.ofNullable(users.get(id.value())).map(user -> copy(user.id(), user));
    }

    @Override
    public Optional<User> inOrganization(OrganizationId organizationId, UserId id) {
        return byId(id).filter(user -> user.organizationId().equals(organizationId));
    }

    @Override
    public boolean emailTaken(EmailAddress email) {
        return users.values().stream().anyMatch(user -> user.email().equals(email));
    }

    @Override
    public boolean existsInOrganization(OrganizationId organizationId, UserId id) {
        return inOrganization(organizationId, id).isPresent();
    }

    @Override
    public List<User> inOrganization(OrganizationId organizationId) {
        return users.values().stream().filter(user -> user.organizationId().equals(organizationId))
                .sorted(Comparator.comparing(User::name)).map(user -> copy(user.id(), user)).toList();
    }

    @Override
    public int countInOrganization(OrganizationId organizationId) {
        return inOrganization(organizationId).size();
    }

    @Override
    public void delete(UserId id) {
        users.remove(id.value());
    }

    @Override
    public Organization save(Organization organization) {
        OrganizationId id = organization.id() == null ? new OrganizationId(nextId("org")) : organization.id();
        Organization stored = Organization.restore(id, organization.name(), organization.plan(),
                organization.trialEndsAt());
        organizations.put(id.value(), stored);
        return Organization.restore(id, stored.name(), stored.plan(), stored.trialEndsAt());
    }

    @Override
    public Optional<Organization> byId(OrganizationId id) {
        return Optional.ofNullable(organizations.get(id.value())).map(organization -> Organization.restore(
                organization.id(), organization.name(), organization.plan(), organization.trialEndsAt()));
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        String id = token.id() == null ? nextId("token") : token.id();
        tokens.put(id, RefreshToken.restore(id, token.userId(), token.tokenHash(), token.expiresAt(),
                token.revokedAt()));
        return tokens.get(id);
    }

    @Override
    public Optional<RefreshToken> byHash(String tokenHash) {
        return tokens.values().stream().filter(token -> token.tokenHash().equals(tokenHash)).findFirst()
                .map(token -> RefreshToken.restore(token.id(), token.userId(), token.tokenHash(),
                        token.expiresAt(), token.revokedAt()));
    }

    @Override
    public int revokeAll(UserId userId, Instant now) {
        int revoked = 0;
        for (RefreshToken token : tokens.values()) {
            if (token.userId().equals(userId) && !token.isRevoked()) {
                token.revoke(now);
                revoked++;
            }
        }
        return revoked;
    }

    @Override
    public UserInvite save(UserInvite invite) {
        InviteId id = invite.id() == null ? new InviteId(nextId("invite")) : invite.id();
        UserInvite stored = copy(id, invite);
        invites.put(id.value(), stored);
        return copy(id, stored);
    }

    private UserInvite copy(InviteId id, UserInvite invite) {
        return UserInvite.restore(id, invite.organizationId(), invite.name(), invite.email(), invite.role(),
                invite.token(), invite.expiresAt(), invite.acceptedAt(),
                invite.createdAt() == null ? CREATED : invite.createdAt());
    }

    @Override
    public Optional<UserInvite> byToken(String token) {
        return invites.values().stream().filter(invite -> invite.token().equals(token)).findFirst()
                .map(invite -> copy(invite.id(), invite));
    }

    @Override
    public Optional<UserInvite> inOrganization(OrganizationId organizationId, InviteId id) {
        return Optional.ofNullable(invites.get(id.value()))
                .filter(invite -> invite.organizationId().equals(organizationId))
                .map(invite -> copy(invite.id(), invite));
    }

    @Override
    public List<UserInvite> of(OrganizationId organizationId) {
        return invites.values().stream()
                .filter(invite -> invite.organizationId().equals(organizationId) && invite.acceptedAt() == null)
                .map(invite -> copy(invite.id(), invite)).toList();
    }

    @Override
    public boolean exists(EmailAddress email, OrganizationId organizationId, Instant now) {
        return invites.values().stream().anyMatch(invite -> invite.email().equals(email)
                && invite.organizationId().equals(organizationId) && invite.acceptedAt() == null
                && invite.expiresAt().isAfter(now));
    }

    @Override
    public void delete(OrganizationId organizationId, InviteId id) {
        UserInvite invite = invites.get(id.value());
        if (invite != null && invite.organizationId().equals(organizationId)) {
            invites.remove(id.value());
        }
    }
}
