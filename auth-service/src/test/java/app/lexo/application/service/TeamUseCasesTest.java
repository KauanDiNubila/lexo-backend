package app.lexo.application.service;

import app.lexo.application.port.in.AcceptInviteCommand;
import app.lexo.application.port.in.Actor;
import app.lexo.application.port.in.ChangeUserRoleCommand;
import app.lexo.application.port.in.InviteResponse;
import app.lexo.application.port.in.InviteUserCommand;
import app.lexo.application.port.in.RemoveUserCommand;
import app.lexo.application.port.in.RevokeInviteCommand;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.InviteEmail;
import app.lexo.common.error.ConflictException;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.event.EventType;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.EmailAlreadyInUseException;
import app.lexo.domain.InviteId;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.OrganizationNotFoundException;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.User;
import app.lexo.domain.UserInvite;
import app.lexo.domain.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Team use cases")
class TeamUseCasesTest {

    private static final EmailAddress NEW_EMAIL = new EmailAddress("carla@escritorio.com");

    private AuthHarness h;
    private InviteUserService invite;
    private RevokeInviteService revoke;
    private ChangeUserRoleService changeRole;
    private RemoveUserService remove;
    private AcceptInviteService accept;
    private Organization organization;
    private User admin;
    private Actor actor;

    @BeforeEach
    void wire() {
        h = new AuthHarness();
        invite = new InviteUserService(h.store, h.store, h.store, h.store, h.emails, h.events, Fakes.CLOCK);
        revoke = new RevokeInviteService(h.store, h.store, h.events);
        changeRole = new ChangeUserRoleService(h.store, h.store, h.store, h.cutoffs, h.events, Fakes.CLOCK);
        remove = new RemoveUserService(h.store, h.store, h.store, h.cutoffs, h.events, Fakes.CLOCK);
        accept = new AcceptInviteService(h.store, h.store, h.store, h.store, h.hasher, Fakes.CLOCK);
        organization = h.addOrganization("Escritorio Silva");
        admin = h.addUser(organization.id(), "Ana Admin", "ana@escritorio.com", Role.ADMIN);
        actor = AuthHarness.actor(admin);
    }

    private InviteResponse inviteCarla() {
        return invite.invite(new InviteUserCommand(organization.id(), actor, "Carla Nova", NEW_EMAIL,
                Role.SECRETARIA));
    }

    private UserInvite storedInvite(InviteResponse response) {
        return h.store.invites.get(response.id());
    }

    @Test
    @DisplayName("inviting stores a pending invite for seven days, mails it and records the event")
    void invites() {
        InviteResponse response = inviteCarla();

        UserInvite stored = storedInvite(response);
        assertThat(stored.organizationId()).isEqualTo(organization.id());
        assertThat(stored.role()).isEqualTo(Role.SECRETARIA);
        assertThat(stored.expiresAt()).isEqualTo(Fakes.NOW.plus(7, ChronoUnit.DAYS));
        assertThat(response.expiresAt()).isEqualTo(stored.expiresAt());
        assertThat(h.emails.sent).containsExactly(
                new InviteEmail(NEW_EMAIL, "Carla Nova", "Escritorio Silva", stored.token()));
        assertThat(h.events.published).hasSize(1);
        AccountEvent event = h.events.published.get(0);
        assertThat(event.type()).isEqualTo(EventType.USER_INVITED);
        assertThat(event.entityType()).isEqualTo("USUARIO");
        assertThat(event.entityId()).isNull();
        assertThat(event.description()).isEqualTo("Convidou Carla Nova (carla@escritorio.com) como SECRETARIA");
        assertThat(event.actor()).isEqualTo(actor);
    }

    @Test
    @DisplayName("inviting an email that already has an account is a conflict and sends nothing")
    void invitingExistingUser() {
        h.addUser(organization.id(), "Carla", NEW_EMAIL.value(), Role.ADVOGADO);

        assertThatThrownBy(this::inviteCarla).isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um usuário com este email");

        assertThat(h.store.invites).isEmpty();
        assertThat(h.emails.sent).isEmpty();
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("inviting twice is a conflict while the first invite is pending, not after it expired")
    void invitingTwice() {
        InviteResponse first = inviteCarla();

        assertThatThrownBy(this::inviteCarla).isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um convite pendente para este email");

        UserInvite expired = storedInvite(first);
        h.store.invites.put(first.id(), UserInvite.restore(expired.id(), expired.organizationId(), expired.name(),
                expired.email(), expired.role(), expired.token(), Fakes.NOW.minusSeconds(1), null,
                expired.createdAt()));
        assertThat(inviteCarla().id()).isNotEqualTo(first.id());
    }

    @Test
    @DisplayName("a pending invite of another organization does not block the invitation")
    void pendingInviteIsPerOrganization() {
        Organization other = h.addOrganization("Outro");
        User otherAdmin = h.addUser(other.id(), "Outro Admin", "outro@escritorio.com", Role.ADMIN);
        invite.invite(new InviteUserCommand(other.id(), AuthHarness.actor(otherAdmin), "Carla", NEW_EMAIL,
                Role.ADVOGADO));

        assertThat(inviteCarla()).isNotNull();
    }

    @Test
    @DisplayName("inviting for an organization that does not exist fails before anything is sent")
    void invitingForMissingOrganization() {
        assertThatThrownBy(() -> invite.invite(new InviteUserCommand(new OrganizationId("missing"), actor,
                "Carla Nova", NEW_EMAIL, Role.SECRETARIA))).isInstanceOf(OrganizationNotFoundException.class);

        assertThat(h.emails.sent).isEmpty();
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("revoking deletes the invite and records the event")
    void revokes() {
        InviteResponse response = inviteCarla();
        h.events.published.clear();

        revoke.revoke(new RevokeInviteCommand(organization.id(), actor, new InviteId(response.id())));

        assertThat(h.store.invites).isEmpty();
        assertThat(h.events.published).hasSize(1);
        assertThat(h.events.published.get(0).type()).isEqualTo(EventType.INVITE_REVOKED);
        assertThat(h.events.published.get(0).description()).isEqualTo("Revogou convite de carla@escritorio.com");
    }

    @Test
    @DisplayName("revoking an unknown invite or one of another organization changes nothing")
    void revokingForeignInvite() {
        InviteResponse response = inviteCarla();
        h.events.published.clear();
        Organization other = h.addOrganization("Outro");

        revoke.revoke(new RevokeInviteCommand(other.id(), actor, new InviteId(response.id())));
        revoke.revoke(new RevokeInviteCommand(organization.id(), actor, new InviteId("unknown")));

        assertThat(h.store.invites).hasSize(1);
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("changing a role saves it, ends the sessions of that user and records the event")
    void changesRole() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        h.sessions.start(member);
        h.sessions.start(member);

        changeRole.changeRole(new ChangeUserRoleCommand(organization.id(), actor, member.id(), Role.SECRETARIA));

        assertThat(h.store.byId(member.id()).orElseThrow().role()).isEqualTo(Role.SECRETARIA);
        assertThat(h.store.tokens.values()).allMatch(RefreshToken::isRevoked);
        AccountEvent event = h.events.published.get(0);
        assertThat(event.type()).isEqualTo(EventType.ROLE_CHANGED);
        assertThat(event.entityId()).isEqualTo(member.id().value());
        assertThat(event.description()).isEqualTo("Alterou papel do usuário para SECRETARIA");
    }

    @Test
    @DisplayName("changing a role cuts off the access tokens issued so far for that user only")
    void changingRoleCutsOffAccessTokens() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);

        changeRole.changeRole(new ChangeUserRoleCommand(organization.id(), actor, member.id(), Role.SECRETARIA));

        assertThat(h.cutoffs.revoked).containsOnlyKeys(member.id());
        assertThat(h.cutoffs.revoked.get(member.id())).isEqualTo(Fakes.NOW);
    }

    @Test
    @DisplayName("a role change that cannot cut off the access tokens fails before recording the event")
    void roleChangeFailsWhenTheCutoffFails() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        h.cutoffs.failing = true;

        assertThatThrownBy(() -> changeRole.changeRole(
                new ChangeUserRoleCommand(organization.id(), actor, member.id(), Role.SECRETARIA)))
                .isInstanceOf(ServiceUnavailableException.class);

        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("sessions of other users are not touched when a role changes")
    void changingRoleKeepsOtherSessions() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        h.sessions.start(admin);

        changeRole.changeRole(new ChangeUserRoleCommand(organization.id(), actor, member.id(), Role.SECRETARIA));

        assertThat(h.store.tokens.values()).noneMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("an admin cannot change their own role")
    void cannotChangeOwnRole() {
        assertThatThrownBy(() -> changeRole.changeRole(
                new ChangeUserRoleCommand(organization.id(), actor, admin.id(), Role.ADVOGADO)))
                .isInstanceOf(InvalidDataException.class).hasMessage("Você não pode alterar seu próprio papel");
        assertThat(h.store.byId(admin.id()).orElseThrow().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("changing the role of a user of another organization is not found")
    void changingForeignRole() {
        Organization other = h.addOrganization("Outro");
        User foreign = h.addUser(other.id(), "Outro", "outro@escritorio.com", Role.ADMIN);

        assertThatThrownBy(() -> changeRole.changeRole(
                new ChangeUserRoleCommand(organization.id(), actor, foreign.id(), Role.ADVOGADO)))
                .isInstanceOf(UserNotFoundException.class);
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("removing deletes the user, ends their sessions and records the event")
    void removes() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        h.sessions.start(member);

        remove.remove(new RemoveUserCommand(organization.id(), actor, member.id()));

        assertThat(h.store.byId(member.id())).isEmpty();
        assertThat(h.store.tokens.values()).allMatch(RefreshToken::isRevoked);
        AccountEvent event = h.events.published.get(0);
        assertThat(event.type()).isEqualTo(EventType.USER_REMOVED);
        assertThat(event.entityId()).isEqualTo(member.id().value());
        assertThat(event.description()).isEqualTo("Removeu Bruno (bruno@escritorio.com)");
    }

    @Test
    @DisplayName("removing a user cuts off the access tokens issued so far, and fails if it cannot")
    void removingCutsOffAccessTokens() {
        User member = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        User other = h.addUser(organization.id(), "Carlos", "carlos@escritorio.com", Role.ADVOGADO);

        remove.remove(new RemoveUserCommand(organization.id(), actor, member.id()));

        assertThat(h.cutoffs.revoked).containsOnlyKeys(member.id());
        assertThat(h.cutoffs.revoked.get(member.id())).isEqualTo(Fakes.NOW);

        h.cutoffs.failing = true;
        h.events.published.clear();
        assertThatThrownBy(() -> remove.remove(new RemoveUserCommand(organization.id(), actor, other.id())))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("inviting fails when the invitation email cannot be queued, without recording the event")
    void invitingFailsWhenTheEmailCannotBeQueued() {
        InviteUserService failing = new InviteUserService(h.store, h.store, h.store, h.store,
                new Fakes.FailingInviteEmails(), h.events, Fakes.CLOCK);

        assertThatThrownBy(() -> failing.invite(new InviteUserCommand(organization.id(), actor, "Carla Nova",
                NEW_EMAIL, Role.SECRETARIA))).isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Não foi possível enviar o convite agora. Tente novamente em instantes.");

        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("accepting an invite that loses the race for the email is a conflict")
    void acceptLosingTheRaceIsAConflict() {
        String token = pendingToken();
        AcceptInviteService racing = new AcceptInviteService(h.store, h.store, h.store, user -> {
            throw new EmailAlreadyInUseException();
        }, h.hasher, Fakes.CLOCK);

        assertThatThrownBy(() -> racing.accept(new AcceptInviteCommand(token, "new-password-1", "new-password-1")))
                .isInstanceOf(ConflictException.class).hasMessage("Já existe uma conta com este email");
        assertThat(h.store.byToken(token).orElseThrow().acceptedAt()).isNull();
    }

    @Test
    @DisplayName("an admin cannot remove themselves, and users of other organizations are not found")
    void removeGuards() {
        Organization other = h.addOrganization("Outro");
        User foreign = h.addUser(other.id(), "Outro", "outro@escritorio.com", Role.ADMIN);

        assertThatThrownBy(() -> remove.remove(new RemoveUserCommand(organization.id(), actor, admin.id())))
                .isInstanceOf(InvalidDataException.class).hasMessage("Você não pode remover a si mesmo");
        assertThatThrownBy(() -> remove.remove(new RemoveUserCommand(organization.id(), actor, foreign.id())))
                .isInstanceOf(UserNotFoundException.class);
        assertThat(h.store.users).hasSize(2);
    }

    private String pendingToken() {
        return storedInvite(inviteCarla()).token();
    }

    @Test
    @DisplayName("accepting creates the user with the invited role and marks the invite as used")
    void accepts() {
        String token = pendingToken();

        accept.accept(new AcceptInviteCommand(token, "new-password-1", "new-password-1"));

        User created = h.store.byEmail(NEW_EMAIL).orElseThrow();
        assertThat(created.organizationId()).isEqualTo(organization.id());
        assertThat(created.name()).isEqualTo("Carla Nova");
        assertThat(created.role()).isEqualTo(Role.SECRETARIA);
        assertThat(created.passwordHash()).isEqualTo("hashed:new-password-1");
        assertThat(h.store.byToken(token).orElseThrow().acceptedAt()).isEqualTo(Fakes.NOW);
    }

    @Test
    @DisplayName("accepting checks the passwords first, then the invite")
    void acceptChecksPasswordsFirst() {
        assertThatThrownBy(() -> accept.accept(new AcceptInviteCommand("unknown", "new-password-1", "other-1234")))
                .isInstanceOf(InvalidDataException.class).hasMessage("As senhas não coincidem");
        assertThatThrownBy(() -> accept.accept(new AcceptInviteCommand("unknown", "new-password-1",
                "new-password-1"))).isInstanceOf(InvalidDataException.class).hasMessage("Convite inválido");
    }

    @Test
    @DisplayName("an invite can be used only once")
    void acceptsOnce() {
        String token = pendingToken();
        accept.accept(new AcceptInviteCommand(token, "new-password-1", "new-password-1"));

        assertThatThrownBy(() -> accept.accept(new AcceptInviteCommand(token, "new-password-1", "new-password-1")))
                .isInstanceOf(InvalidDataException.class).hasMessage("Este convite já foi utilizado");
    }

    @Test
    @DisplayName("an expired invite is refused")
    void acceptRefusesExpired() {
        InviteResponse response = inviteCarla();
        UserInvite stored = storedInvite(response);
        h.store.invites.put(response.id(), UserInvite.restore(stored.id(), stored.organizationId(), stored.name(),
                stored.email(), stored.role(), stored.token(), Fakes.NOW.minusSeconds(1), null, stored.createdAt()));

        assertThatThrownBy(() -> accept.accept(new AcceptInviteCommand(stored.token(), "new-password-1",
                "new-password-1"))).isInstanceOf(InvalidDataException.class).hasMessage("Este convite expirou");
        assertThat(h.store.byEmail(NEW_EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("accepting is a conflict when the email already has an account, and the invite stays open")
    void acceptConflicts() {
        String token = pendingToken();
        h.addUser(h.addOrganization("Outro").id(), "Carla", NEW_EMAIL.value(), Role.ADVOGADO);

        assertThatThrownBy(() -> accept.accept(new AcceptInviteCommand(token, "new-password-1", "new-password-1")))
                .isInstanceOf(ConflictException.class).hasMessage("Já existe uma conta com este email");
        assertThat(h.store.byToken(token).orElseThrow().acceptedAt()).isNull();
    }
}
