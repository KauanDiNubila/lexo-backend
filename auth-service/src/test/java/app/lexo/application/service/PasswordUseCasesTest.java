package app.lexo.application.service;

import app.lexo.application.port.in.ChangePasswordCommand;
import app.lexo.application.port.in.RequestPasswordResetCommand;
import app.lexo.application.port.in.ResetPasswordCommand;
import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.PasswordResetEmail;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.error.TooManyRequestsException;
import app.lexo.common.event.EventType;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.PasswordReset;
import app.lexo.domain.TokenSecret;
import app.lexo.domain.User;
import app.lexo.domain.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password use cases")
class PasswordUseCasesTest {

    private static final String EMAIL = "ana@escritorio.com";
    private static final String NEW_PASSWORD = "another-password-2";

    private AuthHarness h;
    private RequestPasswordResetService request;
    private ResetPasswordService reset;
    private ChangePasswordService change;
    private Organization organization;
    private User user;

    @BeforeEach
    void wire() {
        h = new AuthHarness();
        request = new RequestPasswordResetService(h.store, h.attempts, h.store, h.store, h.passwordEmails,
                Fakes.CLOCK);
        reset = new ResetPasswordService(h.store, h.store, h.store, h.store, h.hasher, h.terminator, h.events,
                h.passwordEmails, Fakes.CLOCK);
        change = new ChangePasswordService(h.store, h.store, h.attempts, h.hasher, h.terminator, h.events,
                h.passwordEmails);
        organization = h.addOrganization("Escritorio");
        user = h.addUser(organization.id(), "Ana", EMAIL, Role.ADVOGADO);
    }

    private String requestLink() {
        request.request(new RequestPasswordResetCommand(new EmailAddress(EMAIL)));
        return h.passwordEmails.links.get(h.passwordEmails.links.size() - 1).token();
    }

    private void resetWith(String token, String password, String confirmation) {
        reset.reset(new ResetPasswordCommand(token, password, confirmation));
    }

    private void changeTo(String current, String password, String confirmation) {
        change.change(new ChangePasswordCommand(organization.id(), AuthHarness.actor(user), current, password,
                confirmation));
    }

    private User stored() {
        return h.store.byId(user.id()).orElseThrow();
    }

    private boolean everySessionEnded() {
        return h.store.tokens.values().stream().allMatch(token -> token.isRevoked())
                && h.cutoffs.revoked.get(user.id()) != null;
    }

    @Test
    @DisplayName("requesting stores only the hash of the token, valid for one hour, and emails the token")
    void requestIssuesAHashedToken() {
        String token = requestLink();

        assertThat(h.store.passwordResets).hasSize(1);
        PasswordReset stored = h.store.passwordResets.values().iterator().next();
        assertThat(stored.userId()).isEqualTo(user.id());
        assertThat(stored.tokenHash()).isEqualTo(TokenSecret.hash(token)).isNotEqualTo(token);
        assertThat(stored.expiresAt()).isEqualTo(Fakes.NOW.plus(Duration.ofHours(1)));
        PasswordResetEmail email = h.passwordEmails.links.get(0);
        assertThat(email.to()).isEqualTo(new EmailAddress(EMAIL));
        assertThat(email.name()).isEqualTo("Ana");
        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("requesting for an email without an account does nothing and does not fail")
    void requestForAnUnknownEmail() {
        request.request(new RequestPasswordResetCommand(new EmailAddress("nobody@escritorio.com")));

        assertThat(h.store.passwordResets).isEmpty();
        assertThat(h.passwordEmails.links).isEmpty();
    }

    @Test
    @DisplayName("a new request replaces the previous token")
    void newRequestReplacesThePreviousToken() {
        String first = requestLink();
        String second = requestLink();

        assertThat(h.store.passwordResets).hasSize(1);
        assertThatThrownBy(() -> resetWith(first, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class);
        resetWith(second, NEW_PASSWORD, NEW_PASSWORD);
        assertThat(h.hasher.matches(NEW_PASSWORD, stored().passwordHash())).isTrue();
    }

    @Test
    @DisplayName("requests are limited to three per hour for each email, known or not")
    void requestsAreLimited() {
        for (int i = 0; i < 3; i++) {
            requestLink();
        }
        request.request(new RequestPasswordResetCommand(new EmailAddress(EMAIL)));
        request.request(new RequestPasswordResetCommand(new EmailAddress("nobody@escritorio.com")));

        assertThat(h.passwordEmails.links).hasSize(3);
        assertThat(h.attempts.windows.get("password-reset:email:" + EMAIL)).isEqualTo(Duration.ofHours(1));
        assertThat(h.attempts.registered).contains("password-reset:email:nobody@escritorio.com");
    }

    @Test
    @DisplayName("resetting changes the password, ends every session, records the event and warns by email")
    void resetChangesThePassword() {
        h.sessions.start(user);
        String token = requestLink();

        resetWith(token, NEW_PASSWORD, NEW_PASSWORD);

        assertThat(h.hasher.matches(NEW_PASSWORD, stored().passwordHash())).isTrue();
        assertThat(h.hasher.matches(AuthHarness.PASSWORD, stored().passwordHash())).isFalse();
        assertThat(everySessionEnded()).isTrue();
        assertThat(h.cutoffs.revoked.get(user.id())).isEqualTo(Fakes.NOW);
        assertThat(h.store.passwordResets).isEmpty();
        assertThat(h.events.published).hasSize(1);
        AccountEvent event = h.events.published.get(0);
        assertThat(event.type()).isEqualTo(EventType.PASSWORD_RESET);
        assertThat(event.organizationId()).isEqualTo(organization.id());
        assertThat(event.actor().userId()).isEqualTo(user.id());
        assertThat(event.actor().name()).isEqualTo("Ana");
        assertThat(h.passwordEmails.notices).hasSize(1);
        assertThat(h.passwordEmails.notices.get(0).to()).isEqualTo(new EmailAddress(EMAIL));
    }

    @Test
    @DisplayName("a token works only once")
    void tokenWorksOnlyOnce() {
        String token = requestLink();
        resetWith(token, NEW_PASSWORD, NEW_PASSWORD);

        assertThatThrownBy(() -> resetWith(token, "third-password-3", "third-password-3"))
                .isInstanceOf(InvalidDataException.class).hasMessage("Link inválido ou expirado");
        assertThat(h.hasher.matches(NEW_PASSWORD, stored().passwordHash())).isTrue();
    }

    @Test
    @DisplayName("a token is valid up to its expiry instant and refused after it")
    void tokenExpires() {
        h.store.save(PasswordReset.restore(null, user.id(), TokenSecret.hash("on-the-limit"), Fakes.NOW));
        h.store.save(PasswordReset.restore(null, user.id(), TokenSecret.hash("too-late"),
                Fakes.NOW.minusSeconds(1)));

        assertThatThrownBy(() -> resetWith("too-late", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class).hasMessage("Link inválido ou expirado");
        resetWith("on-the-limit", NEW_PASSWORD, NEW_PASSWORD);
        assertThat(h.hasher.matches(NEW_PASSWORD, stored().passwordHash())).isTrue();
    }

    @Test
    @DisplayName("an unknown token and the token of a removed user are refused with the same message")
    void unknownTokens() {
        String token = requestLink();
        h.store.delete(organization.id(), user.id());

        assertThatThrownBy(() -> resetWith(token, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class).hasMessage("Link inválido ou expirado");
        assertThatThrownBy(() -> resetWith("never-issued", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class).hasMessage("Link inválido ou expirado");
        assertThat(h.events.published).isEmpty();
        assertThat(h.passwordEmails.notices).isEmpty();
    }

    @Test
    @DisplayName("passwords that do not match are refused and keep the token usable")
    void mismatchKeepsTheToken() {
        String token = requestLink();

        assertThatThrownBy(() -> resetWith(token, NEW_PASSWORD, "something-else-3"))
                .isInstanceOf(PasswordMismatchException.class);

        assertThat(h.store.passwordResets).hasSize(1);
        assertThat(h.hasher.matches(AuthHarness.PASSWORD, stored().passwordHash())).isTrue();
        resetWith(token, NEW_PASSWORD, NEW_PASSWORD);
    }

    @Test
    @DisplayName("resetting the password does not turn two factor off")
    void resetKeepsTwoFactor() {
        User withTwoFactor = stored();
        withTwoFactor.beginTwoFactorSetup("enc:SECRET1");
        withTwoFactor.confirmTwoFactorSetup();
        h.store.save(withTwoFactor);
        String token = requestLink();

        resetWith(token, NEW_PASSWORD, NEW_PASSWORD);

        assertThat(stored().twoFactorEnabled()).isTrue();
        assertThat(stored().twoFactorSecret()).isEqualTo("enc:SECRET1");
    }

    @Test
    @DisplayName("changing checks the current password, replaces it, ends every session and records the event")
    void changeChangesThePassword() {
        h.sessions.start(user);

        changeTo(AuthHarness.PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

        assertThat(h.hasher.matches(NEW_PASSWORD, stored().passwordHash())).isTrue();
        assertThat(everySessionEnded()).isTrue();
        assertThat(h.attempts.reset).containsExactly("password-change:" + user.id().value());
        assertThat(h.events.published).hasSize(1);
        AccountEvent event = h.events.published.get(0);
        assertThat(event.type()).isEqualTo(EventType.PASSWORD_CHANGED);
        assertThat(event.organizationId()).isEqualTo(organization.id());
        assertThat(event.actor().userId()).isEqualTo(user.id());
        assertThat(h.passwordEmails.notices).hasSize(1);
    }

    @Test
    @DisplayName("a wrong current password changes nothing")
    void wrongCurrentPassword() {
        h.sessions.start(user);

        assertThatThrownBy(() -> changeTo("not-my-password", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class).hasMessage("Senha atual incorreta");

        assertThat(h.hasher.matches(AuthHarness.PASSWORD, stored().passwordHash())).isTrue();
        assertThat(h.store.tokens.values()).noneMatch(token -> token.isRevoked());
        assertThat(h.cutoffs.revoked).isEmpty();
        assertThat(h.events.published).isEmpty();
        assertThat(h.passwordEmails.notices).isEmpty();
    }

    @Test
    @DisplayName("a confirmation that does not match is refused before any attempt is counted")
    void changeMismatch() {
        assertThatThrownBy(() -> changeTo(AuthHarness.PASSWORD, NEW_PASSWORD, "something-else-3"))
                .isInstanceOf(PasswordMismatchException.class);

        assertThat(h.attempts.registered).isEmpty();
        assertThat(h.hasher.matches(AuthHarness.PASSWORD, stored().passwordHash())).isTrue();
    }

    @Test
    @DisplayName("the new password must differ from the current one")
    void changeToTheSamePassword() {
        assertThatThrownBy(() -> changeTo(AuthHarness.PASSWORD, AuthHarness.PASSWORD, AuthHarness.PASSWORD))
                .isInstanceOf(InvalidDataException.class).hasMessage("A nova senha deve ser diferente da atual");

        assertThat(h.events.published).isEmpty();
    }

    @Test
    @DisplayName("after five attempts in fifteen minutes even the right password is refused")
    void changeIsLimited() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> changeTo("not-my-password", NEW_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidDataException.class);
        }

        assertThatThrownBy(() -> changeTo(AuthHarness.PASSWORD, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(TooManyRequestsException.class);

        assertThat(h.hasher.matches(AuthHarness.PASSWORD, stored().passwordHash())).isTrue();
        assertThat(h.attempts.windows.get("password-change:" + user.id().value()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("a session user that no longer exists in the organization is not found")
    void changeForAnotherOrganization() {
        OrganizationId other = h.addOrganization("Outro").id();

        assertThatThrownBy(() -> change.change(new ChangePasswordCommand(other, AuthHarness.actor(user),
                AuthHarness.PASSWORD, NEW_PASSWORD, NEW_PASSWORD))).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("a command rejects a short new password and a blank current password")
    void commandsValidateInput() {
        assertThatThrownBy(() -> new ResetPasswordCommand("token", "short", "short"))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new ResetPasswordCommand(" ", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new ChangePasswordCommand(organization.id(), AuthHarness.actor(user), " ",
                NEW_PASSWORD, NEW_PASSWORD)).isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new ChangePasswordCommand(organization.id(), AuthHarness.actor(user),
                AuthHarness.PASSWORD, "short", "short")).isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("a token that expired long ago is refused even for an existing user")
    void ancientToken() {
        h.store.save(PasswordReset.restore(null, user.id(), TokenSecret.hash("ancient"),
                Instant.parse("2020-01-01T00:00:00Z")));

        assertThatThrownBy(() -> resetWith("ancient", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(InvalidDataException.class);
    }
}
