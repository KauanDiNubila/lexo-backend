package app.lexo.application.service;

import app.lexo.application.port.in.AccountResponse;
import app.lexo.application.port.in.InviteInfo;
import app.lexo.application.port.in.InviteResponse;
import app.lexo.application.port.in.InviteUserCommand;
import app.lexo.application.port.in.OrganizationInfo;
import app.lexo.application.port.in.ProfileInfo;
import app.lexo.application.port.in.RenameOrganizationCommand;
import app.lexo.application.port.in.RenameProfileCommand;
import app.lexo.application.port.in.UserResponse;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.OrganizationNotFoundException;
import app.lexo.domain.User;
import app.lexo.domain.UserInvite;
import app.lexo.domain.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account and query services")
class QueryServicesTest {

    private AuthHarness h;
    private AccountQueryService account;
    private RenameOrganizationService renameOrganization;
    private RenameProfileService renameProfile;
    private UserQueryService users;
    private InviteQueryService invites;
    private Organization organization;
    private User admin;

    @BeforeEach
    void wire() {
        h = new AuthHarness();
        account = new AccountQueryService(h.store, h.store, h.store);
        renameOrganization = new RenameOrganizationService(h.store, h.store, h.store);
        renameProfile = new RenameProfileService(h.store, h.store);
        users = new UserQueryService(h.store, h.store, h.store);
        invites = new InviteQueryService(h.store, h.store, h.store, Fakes.CLOCK);
        organization = h.addOrganization("Escritorio Silva");
        admin = h.addUser(organization.id(), "Ana Admin", "ana@escritorio.com", Role.ADMIN);
    }

    @Test
    @DisplayName("the account shows the organization with its member count and the profile")
    void showsAccount() {
        h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);
        h.addUser(h.addOrganization("Outro").id(), "Outro", "outro@escritorio.com", Role.ADMIN);

        AccountResponse response = account.get(organization.id(), admin.id());

        assertThat(response.organization().members()).isEqualTo(2);
        assertThat(response.organization().name()).isEqualTo("Escritorio Silva");
        assertThat(response.organization().plan()).isEqualTo("trial");
        assertThat(response.user().id()).isEqualTo(admin.id().value());
        assertThat(response.user().totpEnabled()).isFalse();
    }

    @Test
    @DisplayName("the account reports a missing organization before a missing user")
    void accountNotFound() {
        assertThatThrownBy(() -> account.get(new OrganizationId("missing"), AuthHarness.unknownUser()))
                .isInstanceOf(OrganizationNotFoundException.class).hasMessage("Escritório não encontrado");
        assertThatThrownBy(() -> account.get(organization.id(), AuthHarness.unknownUser()))
                .isInstanceOf(UserNotFoundException.class).hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("renaming the organization trims, saves and returns the member count")
    void renamesOrganization() {
        OrganizationInfo info = renameOrganization.rename(
                new RenameOrganizationCommand(organization.id(), "  Novo Nome "));

        assertThat(info.name()).isEqualTo("Novo Nome");
        assertThat(info.members()).isEqualTo(1);
        assertThat(h.store.organizations.get(organization.id().value()).name()).isEqualTo("Novo Nome");
        assertThatThrownBy(() -> renameOrganization.rename(
                new RenameOrganizationCommand(new OrganizationId("missing"), "Nome")))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    @DisplayName("renaming the profile trims and saves only that user")
    void renamesProfile() {
        User other = h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.ADVOGADO);

        ProfileInfo info = renameProfile.rename(new RenameProfileCommand(organization.id(), other.id(), " Bruno S "));

        assertThat(info.name()).isEqualTo("Bruno S");
        assertThat(h.store.users.get(other.id().value()).name()).isEqualTo("Bruno S");
        assertThat(h.store.users.get(admin.id().value()).name()).isEqualTo("Ana Admin");
        assertThatThrownBy(() -> renameProfile.rename(
                new RenameProfileCommand(organization.id(), AuthHarness.unknownUser(), "Nome")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("listing users is scoped to the organization and ordered by name")
    void listsUsers() {
        h.addUser(organization.id(), "Carlos", "carlos@escritorio.com", Role.ADVOGADO);
        h.addUser(organization.id(), "Bruno", "bruno@escritorio.com", Role.SECRETARIA);
        h.addUser(h.addOrganization("Outro").id(), "Aaron", "aaron@escritorio.com", Role.ADMIN);

        List<UserResponse> list = users.list(organization.id());

        assertThat(list).extracting(UserResponse::name).containsExactly("Ana Admin", "Bruno", "Carlos");
        assertThat(list.get(1).role()).isEqualTo(Role.SECRETARIA);
    }

    @Test
    @DisplayName("existence and lookup are scoped to the organization")
    void existenceAndLookup() {
        OrganizationId other = h.addOrganization("Outro").id();

        assertThat(users.exists(organization.id(), admin.id())).isTrue();
        assertThat(users.exists(other, admin.id())).isFalse();
        assertThat(users.get(organization.id(), admin.id()).email()).isEqualTo("ana@escritorio.com");
        assertThatThrownBy(() -> users.get(other, admin.id())).isInstanceOf(UserNotFoundException.class);
    }

    private UserInvite invite(String email) {
        InviteUserService service = new InviteUserService(h.store, h.store, h.store, h.store, h.emails, h.events,
                Fakes.CLOCK);
        InviteResponse response = service.invite(new InviteUserCommand(organization.id(), AuthHarness.actor(admin),
                "Carla", new EmailAddress(email), Role.ADVOGADO));
        return h.store.invites.get(response.id());
    }

    @Test
    @DisplayName("pending invites hide the accepted ones and stay inside the organization")
    void listsPendingInvites() {
        UserInvite open = invite("open@escritorio.com");
        UserInvite accepted = invite("accepted@escritorio.com");
        accepted.accept(Fakes.NOW);
        h.store.save(accepted);

        List<InviteResponse> list = invites.list(organization.id());

        assertThat(list).extracting(InviteResponse::id).containsExactly(open.id().value());
        assertThat(invites.list(h.addOrganization("Outro").id())).isEmpty();
    }

    @Test
    @DisplayName("info of a valid invite whose organization is gone is reported as not found")
    void inviteInfoWithoutOrganization() {
        UserInvite orphan = h.store.save(UserInvite.issue(new OrganizationId("missing"), "Carla",
                new EmailAddress("orphan@escritorio.com"), Role.ADVOGADO, Fakes.NOW));

        assertThatThrownBy(() -> invites.get(orphan.token())).isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    @DisplayName("invite info explains why a token cannot be used")
    void inviteInfo() {
        UserInvite valid = invite("valid@escritorio.com");
        UserInvite used = invite("used@escritorio.com");
        used.accept(Fakes.NOW);
        h.store.save(used);
        UserInvite expired = invite("expired@escritorio.com");
        h.store.invites.put(expired.id().value(), UserInvite.restore(expired.id(), expired.organizationId(),
                expired.name(), expired.email(), expired.role(), expired.token(), Fakes.NOW.minusSeconds(1), null,
                expired.createdAt()));

        assertThat(invites.get(valid.token())).isEqualTo(
                new InviteInfo("Escritorio Silva", "Carla", "valid@escritorio.com", true, null));
        assertThat(invites.get(used.token())).isEqualTo(
                new InviteInfo(null, "Carla", "used@escritorio.com", false, "Este convite já foi utilizado"));
        assertThat(invites.get(expired.token())).isEqualTo(
                new InviteInfo(null, "Carla", "expired@escritorio.com", false, "Este convite expirou"));
        assertThat(invites.get("unknown")).isEqualTo(new InviteInfo(null, null, null, false, "Convite inválido"));
    }
}
