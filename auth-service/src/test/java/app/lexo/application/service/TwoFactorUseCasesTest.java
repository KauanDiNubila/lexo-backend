package app.lexo.application.service;

import app.lexo.application.port.in.ConfirmTwoFactorCommand;
import app.lexo.application.port.in.DisableTwoFactorCommand;
import app.lexo.application.port.in.StartTwoFactorCommand;
import app.lexo.application.port.in.TwoFactorSetup;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.event.EventType;
import app.lexo.common.security.Role;
import app.lexo.domain.Organization;
import app.lexo.domain.UnknownSessionUserException;
import app.lexo.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Two factor use cases")
class TwoFactorUseCasesTest {

    private AuthHarness h;
    private StartTwoFactorService start;
    private ConfirmTwoFactorService confirm;
    private DisableTwoFactorService disable;
    private Organization organization;
    private User user;

    @BeforeEach
    void wire() {
        h = new AuthHarness();
        start = new StartTwoFactorService(h.store, h.store, h.cipher, h.totp);
        confirm = new ConfirmTwoFactorService(h.store, h.store, h.cipher, h.totp, h.events);
        disable = new DisableTwoFactorService(h.store, h.store, h.cipher, h.totp, h.events);
        organization = h.addOrganization("Escritorio");
        user = h.addUser(organization.id(), "Ana", "ana@escritorio.com", Role.ADVOGADO);
    }

    private TwoFactorSetup begin() {
        return start.start(new StartTwoFactorCommand(organization.id(), user.id()));
    }

    private void confirmWith(String code) {
        confirm.confirm(new ConfirmTwoFactorCommand(organization.id(), AuthHarness.actor(user), code));
    }

    private void disableWith(String code) {
        disable.disable(new DisableTwoFactorCommand(organization.id(), AuthHarness.actor(user), code));
    }

    @Test
    @DisplayName("starting returns the secret and the link, and stores the secret encrypted as pending")
    void starts() {
        TwoFactorSetup setup = begin();

        assertThat(setup.secret()).isEqualTo("SECRET1");
        assertThat(setup.otpauthUri()).isEqualTo("otpauth://ana@escritorio.com?secret=SECRET1");
        User stored = h.store.byId(user.id()).orElseThrow();
        assertThat(stored.pendingTwoFactorSecret()).contains("enc:SECRET1");
        assertThat(stored.twoFactorEnabled()).isFalse();
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("confirming with the right code turns two factor on and records the event")
    void confirms() {
        begin();

        confirmWith(Fakes.Totp.VALID_CODE);

        User stored = h.store.byId(user.id()).orElseThrow();
        assertThat(stored.twoFactorEnabled()).isTrue();
        assertThat(stored.twoFactorSecret()).isEqualTo("enc:SECRET1");
        assertThat(stored.pendingTwoFactorSecret()).isEmpty();
        assertThat(h.events.published).hasSize(1);
        assertThat(h.events.published.get(0).type()).isEqualTo(EventType.TWO_FACTOR_ENABLED);
        assertThat(h.events.published.get(0).entityType()).isNull();
        assertThat(h.events.published.get(0).description()).isEqualTo("Ativou verificação em dois fatores");
    }

    @Test
    @DisplayName("confirming with a wrong code or without starting is refused")
    void confirmRefused() {
        assertThatThrownBy(() -> confirmWith(Fakes.Totp.VALID_CODE)).isInstanceOf(InvalidDataException.class)
                .hasMessage("Sessão expirada. Reinicie o processo.");

        begin();
        assertThatThrownBy(() -> confirmWith("000000")).isInstanceOf(InvalidDataException.class)
                .hasMessage("Código inválido. Tente novamente.");

        User stored = h.store.byId(user.id()).orElseThrow();
        assertThat(stored.twoFactorEnabled()).isFalse();
        assertThat(stored.pendingTwoFactorSecret()).isPresent();
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("disabling with the right code clears everything and records the event")
    void disables() {
        begin();
        confirmWith(Fakes.Totp.VALID_CODE);
        h.events.published.clear();

        disableWith(Fakes.Totp.VALID_CODE);

        User stored = h.store.byId(user.id()).orElseThrow();
        assertThat(stored.twoFactorEnabled()).isFalse();
        assertThat(stored.twoFactorSecret()).isNull();
        assertThat(h.events.published.get(0).type()).isEqualTo(EventType.TWO_FACTOR_DISABLED);
        assertThat(h.events.published.get(0).description()).isEqualTo("Desativou verificação em dois fatores");
    }

    @Test
    @DisplayName("disabling with a wrong code, or when it is off, is refused")
    void disableRefused() {
        assertThatThrownBy(() -> disableWith(Fakes.Totp.VALID_CODE)).isInstanceOf(InvalidDataException.class)
                .hasMessage("2FA não está ativado.");

        begin();
        confirmWith(Fakes.Totp.VALID_CODE);
        assertThatThrownBy(() -> disableWith("000000")).isInstanceOf(InvalidDataException.class)
                .hasMessage("Código inválido.");
        assertThat(h.store.byId(user.id()).orElseThrow().twoFactorEnabled()).isTrue();
    }

    @Test
    @DisplayName("a caller that does not exist is reported as an unknown session user")
    void unknownCaller() {
        User ghost = User.restore(AuthHarness.unknownUser(), organization.id(), "Ghost", user.email(), "x",
                Role.ADMIN, null, false, null, null);

        assertThatThrownBy(() -> start.start(new StartTwoFactorCommand(organization.id(), ghost.id())))
                .isInstanceOf(UnknownSessionUserException.class);
        assertThatThrownBy(() -> confirm.confirm(new ConfirmTwoFactorCommand(organization.id(),
                AuthHarness.actor(ghost), "123456"))).isInstanceOf(UnknownSessionUserException.class);
        assertThatThrownBy(() -> disable.disable(new DisableTwoFactorCommand(organization.id(),
                AuthHarness.actor(ghost), "123456"))).isInstanceOf(UnknownSessionUserException.class);
    }
}
