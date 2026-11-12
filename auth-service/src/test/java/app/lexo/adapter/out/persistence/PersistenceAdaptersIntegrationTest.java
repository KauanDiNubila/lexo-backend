package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.InviteId;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.User;
import app.lexo.domain.UserId;
import app.lexo.domain.UserInvite;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("Persistence adapters")
class PersistenceAdaptersIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");

    @Autowired
    private UserPersistenceAdapter users;

    @Autowired
    private OrganizationPersistenceAdapter organizations;

    @Autowired
    private RefreshTokenPersistenceAdapter tokens;

    @Autowired
    private InvitePersistenceAdapter invites;

    @Autowired
    private EntityManager entityManager;

    private Organization newOrganization() {
        return organizations.save(Organization.register("Escritorio " + UUID.randomUUID(), NOW));
    }

    private User newUser(OrganizationId organizationId, String name, Role role) {
        return users.save(User.register(organizationId, name,
                new EmailAddress(name.toLowerCase().replace(' ', '.') + "-" + UUID.randomUUID() + "@escritorio.com"),
                "hash", role));
    }

    @Test
    @DisplayName("an organization is saved and loaded back, and renaming persists")
    void organizationRoundTrip() {
        Organization saved = newOrganization();

        Organization loaded = organizations.byId(saved.id()).orElseThrow();
        assertThat(loaded.plan()).isEqualTo("trial");
        assertThat(loaded.trialEndsAt()).isEqualTo(NOW.plusSeconds(30L * 86400));

        loaded.rename("Novo Nome");
        organizations.save(loaded);
        entityManager.flush();
        entityManager.clear();
        assertThat(organizations.byId(saved.id()).orElseThrow().name()).isEqualTo("Novo Nome");
        assertThat(organizations.byId(new OrganizationId("missing"))).isEmpty();
    }

    @Test
    @DisplayName("a user keeps every field, including two factor, through save and load")
    void userRoundTrip() {
        Organization organization = newOrganization();
        User user = newUser(organization.id(), "Ana Admin", Role.ADMIN);
        user.beginTwoFactorSetup("enc:v1:pending");
        users.save(user);
        entityManager.flush();
        entityManager.clear();

        User loaded = users.byId(user.id()).orElseThrow();
        assertThat(loaded.organizationId()).isEqualTo(organization.id());
        assertThat(loaded.role()).isEqualTo(Role.ADMIN);
        assertThat(loaded.pendingTwoFactorSecret()).contains("enc:v1:pending");
        assertThat(loaded.twoFactorEnabled()).isFalse();
        assertThat(loaded.createdAt()).isNotNull();

        loaded.confirmTwoFactorSetup();
        loaded.changeRole(Role.SECRETARIA);
        users.save(loaded);
        entityManager.flush();
        entityManager.clear();
        User confirmed = users.byEmail(user.email()).orElseThrow();
        assertThat(confirmed.twoFactorEnabled()).isTrue();
        assertThat(confirmed.twoFactorSecret()).isEqualTo("enc:v1:pending");
        assertThat(confirmed.pendingTwoFactorSecret()).isEmpty();
        assertThat(confirmed.role()).isEqualTo(Role.SECRETARIA);
    }

    @Test
    @DisplayName("lookups by email and by organization are exact")
    void userLookups() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        User user = newUser(organization.id(), "Ana Admin", Role.ADMIN);

        assertThat(users.emailTaken(user.email())).isTrue();
        assertThat(users.emailTaken(new EmailAddress("nobody@escritorio.com"))).isFalse();
        assertThat(users.inOrganization(organization.id(), user.id())).isPresent();
        assertThat(users.inOrganization(other.id(), user.id())).isEmpty();
        assertThat(users.existsInOrganization(organization.id(), user.id())).isTrue();
        assertThat(users.existsInOrganization(other.id(), user.id())).isFalse();
        assertThat(users.byId(new UserId("missing"))).isEmpty();
    }

    @Test
    @DisplayName("listing and counting stay inside the organization and are ordered by name")
    void userListing() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        newUser(organization.id(), "Carlos", Role.ADVOGADO);
        newUser(organization.id(), "Ana", Role.ADMIN);
        newUser(organization.id(), "Bruno", Role.SECRETARIA);
        newUser(other.id(), "Aaron", Role.ADMIN);

        assertThat(users.inOrganization(organization.id())).extracting(User::name)
                .containsExactly("Ana", "Bruno", "Carlos");
        assertThat(users.countInOrganization(organization.id())).isEqualTo(3);
        assertThat(users.countInOrganization(other.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("deleting a user removes only that user, and only inside the organization")
    void userDeletion() {
        Organization organization = newOrganization();
        User first = newUser(organization.id(), "Ana", Role.ADMIN);
        User second = newUser(organization.id(), "Bruno", Role.ADVOGADO);

        users.delete(newOrganization().id(), first.id());
        entityManager.flush();
        assertThat(users.byId(first.id())).isPresent();

        users.delete(organization.id(), first.id());
        entityManager.flush();

        assertThat(users.byId(first.id())).isEmpty();
        assertThat(users.byId(second.id())).isPresent();
    }

    @Test
    @DisplayName("revoking all tokens of a user only touches that user's tokens that are still active")
    void tokenRevocation() {
        UserId first = new UserId(UUID.randomUUID().toString());
        UserId second = new UserId(UUID.randomUUID().toString());
        RefreshToken active = tokens.save(RefreshToken.issue(first, "hash-" + UUID.randomUUID(), NOW.plusSeconds(60)));
        RefreshToken alreadyRevoked = RefreshToken.issue(first, "hash-" + UUID.randomUUID(), NOW.plusSeconds(60));
        alreadyRevoked.revoke(NOW.minusSeconds(100));
        tokens.save(alreadyRevoked);
        RefreshToken ofOther = tokens.save(RefreshToken.issue(second, "hash-" + UUID.randomUUID(),
                NOW.plusSeconds(60)));
        entityManager.flush();

        int revoked = tokens.revokeAll(first, NOW);
        entityManager.clear();

        assertThat(revoked).isEqualTo(1);
        assertThat(tokens.byHash(active.tokenHash()).orElseThrow().revokedAt()).isEqualTo(NOW);
        assertThat(tokens.byHash(alreadyRevoked.tokenHash()).orElseThrow().revokedAt())
                .isEqualTo(NOW.minusSeconds(100));
        assertThat(tokens.byHash(ofOther.tokenHash()).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("a token is found by its hash and updating it keeps one row")
    void tokenUpdate() {
        RefreshToken saved = tokens.save(RefreshToken.issue(new UserId("u1"), "hash-" + UUID.randomUUID(),
                NOW.plusSeconds(60)));

        RefreshToken loaded = tokens.byHash(saved.tokenHash()).orElseThrow();
        loaded.revoke(NOW);
        tokens.save(loaded);
        entityManager.flush();
        entityManager.clear();

        assertThat(tokens.byHash(saved.tokenHash()).orElseThrow().revokedAt()).isEqualTo(NOW);
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class,
                saved.tokenHash())).isEqualTo(1);
        assertThat(tokens.byHash("unknown")).isEmpty();
    }

    private UserInvite newInvite(OrganizationId organizationId, String email) {
        return invites.save(UserInvite.issue(organizationId, "Carla", new EmailAddress(email), Role.ADVOGADO, NOW));
    }

    @Test
    @DisplayName("an invite keeps its fields and is found by token and by organization")
    void inviteRoundTrip() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        UserInvite saved = newInvite(organization.id(), "carla-" + UUID.randomUUID() + "@escritorio.com");

        UserInvite loaded = invites.byToken(saved.token()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.role()).isEqualTo(Role.ADVOGADO);
        assertThat(loaded.expiresAt()).isEqualTo(NOW.plusSeconds(7L * 86400));
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(invites.inOrganization(organization.id(), saved.id())).isPresent();
        assertThat(invites.inOrganization(other.id(), saved.id())).isEmpty();
        assertThat(invites.byToken("unknown")).isEmpty();
    }

    @Test
    @DisplayName("pending invites are the unaccepted ones of the organization, newest first")
    void pendingInvites() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        UserInvite older = newInvite(organization.id(), "older-" + UUID.randomUUID() + "@escritorio.com");
        UserInvite newer = newInvite(organization.id(), "newer-" + UUID.randomUUID() + "@escritorio.com");
        UserInvite accepted = newInvite(organization.id(), "done-" + UUID.randomUUID() + "@escritorio.com");
        newInvite(other.id(), "other-" + UUID.randomUUID() + "@escritorio.com");
        entityManager.flush();
        jdbc.update("update user_invites set created_at = now() - interval '2 hour' where id = ?",
                older.id().value());
        jdbc.update("update user_invites set created_at = now() - interval '1 hour' where id = ?",
                newer.id().value());
        accepted.accept(NOW);
        invites.save(accepted);
        entityManager.flush();
        entityManager.clear();

        List<UserInvite> pending = invites.of(organization.id());

        assertThat(pending).extracting(invite -> invite.id().value())
                .containsExactly(newer.id().value(), older.id().value());
    }

    @Test
    @DisplayName("a pending invite exists only for an unaccepted, unexpired invite of that organization")
    void pendingInviteExists() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        String email = "carla-" + UUID.randomUUID() + "@escritorio.com";
        UserInvite invite = newInvite(organization.id(), email);
        EmailAddress address = new EmailAddress(email);

        assertThat(invites.exists(address, organization.id(), NOW)).isTrue();
        assertThat(invites.exists(address, other.id(), NOW)).isFalse();
        assertThat(invites.exists(address, organization.id(), invite.expiresAt().plusSeconds(1))).isFalse();
        invite.accept(NOW);
        invites.save(invite);
        assertThat(invites.exists(address, organization.id(), NOW)).isFalse();
    }

    @Test
    @DisplayName("deleting an invite is scoped to the organization")
    void inviteDeletion() {
        Organization organization = newOrganization();
        Organization other = newOrganization();
        UserInvite invite = newInvite(organization.id(), "carla-" + UUID.randomUUID() + "@escritorio.com");

        invites.delete(other.id(), invite.id());
        entityManager.flush();
        assertThat(invites.inOrganization(organization.id(), invite.id())).isPresent();

        invites.delete(organization.id(), invite.id());
        entityManager.flush();
        assertThat(invites.inOrganization(organization.id(), new InviteId(invite.id().value()))).isEmpty();
    }
}
