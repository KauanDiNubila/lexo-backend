package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Command validation")
class CommandValidationTest {

    private static final OrganizationId ORG = new OrganizationId("org-1");
    private static final Actor ACTOR = new Actor(new UserId("u1"), "Ana");
    private static final EmailAddress EMAIL = new EmailAddress("ana@escritorio.com");

    @Test
    @DisplayName("register needs a long enough organization name, name and password")
    void register() {
        assertThatThrownBy(() -> new RegisterOrganizationCommand("A", "Ana", EMAIL, "password-1", "password-1"))
                .isInstanceOf(InvalidDataException.class).hasMessage("Nome do escritório muito curto");
        assertThatThrownBy(() -> new RegisterOrganizationCommand("Escritorio", "A", EMAIL, "password-1", "password-1"))
                .isInstanceOf(InvalidDataException.class).hasMessage("Nome muito curto");
        assertThatThrownBy(() -> new RegisterOrganizationCommand("Escritorio", "Ana", EMAIL, "short", "short"))
                .isInstanceOf(InvalidDataException.class).hasMessage("Senha deve ter ao menos 8 caracteres");
        assertThatCode(() -> new RegisterOrganizationCommand("Es", "An", EMAIL, "12345678", "12345678"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login needs a password")
    void login() {
        assertThatThrownBy(() -> new LoginCommand(EMAIL, " ", null, null)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("renames need a long enough name")
    void renames() {
        assertThatThrownBy(() -> new RenameOrganizationCommand(ORG, "A")).isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new RenameProfileCommand(ORG, new UserId("u1"), " "))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("two factor codes are required")
    void codes() {
        assertThatThrownBy(() -> new ConfirmTwoFactorCommand(ORG, ACTOR, "")).isInstanceOf(InvalidDataException.class)
                .hasMessage("Código é obrigatório");
        assertThatThrownBy(() -> new DisableTwoFactorCommand(ORG, ACTOR, null))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("an invite and a role change need a role")
    void roles() {
        assertThatThrownBy(() -> new InviteUserCommand(ORG, ACTOR, "Carla", EMAIL, null))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new ChangeUserRoleCommand(ORG, ACTOR, new UserId("u2"), null))
                .isInstanceOf(InvalidDataException.class);
        assertThatCode(() -> new InviteUserCommand(ORG, ACTOR, "Carla", EMAIL, Role.ADVOGADO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accepting an invite needs a token and a long enough password")
    void acceptInvite() {
        assertThatThrownBy(() -> new AcceptInviteCommand(" ", "password-1", "password-1"))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new AcceptInviteCommand("token", "short", "short"))
                .isInstanceOf(InvalidDataException.class);
    }
}
