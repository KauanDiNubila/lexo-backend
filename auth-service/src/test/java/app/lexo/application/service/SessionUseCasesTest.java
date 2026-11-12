package app.lexo.application.service;

import app.lexo.application.port.in.LoginCommand;
import app.lexo.application.port.in.LogoutCommand;
import app.lexo.application.port.in.RefreshSessionCommand;
import app.lexo.application.port.in.RegisterOrganizationCommand;
import app.lexo.application.port.in.SessionResponse;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.EmailAlreadyInUseException;
import app.lexo.domain.ExpiredRefreshTokenException;
import app.lexo.domain.InvalidCredentialsException;
import app.lexo.domain.InvalidRefreshTokenException;
import app.lexo.domain.InvalidTwoFactorCodeException;
import app.lexo.domain.Organization;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.RefreshTokenSecret;
import app.lexo.domain.TwoFactorRequiredException;
import app.lexo.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Session use cases")
class SessionUseCasesTest {

    private static final EmailAddress EMAIL = new EmailAddress("ana@escritorio.com");

    private AuthHarness h;
    private RegisterOrganizationService register;
    private LoginService login;
    private RefreshSessionService refresh;
    private LogoutService logout;

    @BeforeEach
    void wire() {
        h = new AuthHarness();
        register = new RegisterOrganizationService(h.store, h.store, h.store, h.hasher, h.sessions, Fakes.CLOCK);
        login = new LoginService(h.store, h.attempts, h.hasher, h.cipher, h.totp, h.sessions);
        refresh = new RefreshSessionService(h.store, h.store, h.store, h.store, h.sessions, Fakes.CLOCK);
        logout = new LogoutService(h.store, h.store, Fakes.CLOCK);
    }

    private RegisterOrganizationCommand registration(String email, String password, String confirmation) {
        return new RegisterOrganizationCommand("Escritorio Silva", "Ana", new EmailAddress(email), password,
                confirmation);
    }

    private LoginCommand loginAs(String password, String totpCode, String address) {
        return new LoginCommand(EMAIL, password, totpCode, address);
    }

    private User userWithTwoFactor() {
        Organization organization = h.addOrganization("Escritorio");
        User user = h.store.save(User.register(organization.id(), "Ana", EMAIL, h.hasher.hash(AuthHarness.PASSWORD),
                Role.ADMIN));
        user.beginTwoFactorSetup(h.cipher.encrypt("SECRET"));
        user.confirmTwoFactorSetup();
        return h.store.save(user);
    }

    @Test
    @DisplayName("register creates the organization on trial, an admin, and a session")
    void registers() {
        SessionResponse session = register.register(registration("Ana@Escritorio.com", AuthHarness.PASSWORD,
                AuthHarness.PASSWORD));

        assertThat(h.store.organizations).hasSize(1);
        Organization organization = h.store.organizations.values().iterator().next();
        assertThat(organization.name()).isEqualTo("Escritorio Silva");
        assertThat(organization.plan()).isEqualTo("trial");
        assertThat(organization.trialEndsAt()).isEqualTo(Fakes.NOW.plus(30, ChronoUnit.DAYS));
        User admin = h.store.users.values().iterator().next();
        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(admin.email()).isEqualTo(EMAIL);
        assertThat(admin.passwordHash()).isEqualTo("hashed:" + AuthHarness.PASSWORD);
        assertThat(admin.organizationId()).isEqualTo(organization.id());
        assertThat(session.user().id()).isEqualTo(admin.id().value());
        assertThat(session.accessToken()).isEqualTo("jwt-for-" + admin.id().value());
    }

    @Test
    @DisplayName("the session refresh token is stored only as a hash and lasts 30 days")
    void storesOnlyTheHash() {
        SessionResponse session = register.register(registration("ana@escritorio.com", AuthHarness.PASSWORD,
                AuthHarness.PASSWORD));

        RefreshToken stored = h.store.tokens.values().iterator().next();
        assertThat(stored.tokenHash()).isEqualTo(RefreshTokenSecret.hash(session.refreshToken()))
                .isNotEqualTo(session.refreshToken());
        assertThat(stored.expiresAt()).isEqualTo(Fakes.NOW.plus(30, ChronoUnit.DAYS));
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("register refuses different passwords and stores nothing")
    void registerRefusesMismatch() {
        assertThatThrownBy(() -> register.register(registration("ana@escritorio.com", AuthHarness.PASSWORD,
                "other-password"))).isInstanceOf(PasswordMismatchException.class);

        assertThat(h.store.users).isEmpty();
        assertThat(h.store.organizations).isEmpty();
    }

    @Test
    @DisplayName("register refuses an email that already has an account, ignoring case")
    void registerRefusesDuplicate() {
        register.register(registration("ana@escritorio.com", AuthHarness.PASSWORD, AuthHarness.PASSWORD));

        assertThatThrownBy(() -> register.register(registration("ANA@escritorio.com", AuthHarness.PASSWORD,
                AuthHarness.PASSWORD))).isInstanceOf(EmailAlreadyInUseException.class);
        assertThat(h.store.users).hasSize(1);
    }

    @Test
    @DisplayName("login returns a session and clears both attempt counters")
    void logsIn() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);

        SessionResponse session = login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10"));

        assertThat(session.user().id()).isEqualTo(user.id().value());
        assertThat(session.user().role()).isEqualTo(Role.ADVOGADO);
        assertThat(h.attempts.reset).containsExactly("login:email:ana@escritorio.com", "login:ip:203.0.113.10");
        assertThat(h.store.tokens).hasSize(1);
    }

    @Test
    @DisplayName("attempts are counted per email and per address in a 15 minute window")
    void countsAttempts() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);

        login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10"));

        assertThat(h.attempts.registered).containsExactly("login:email:ana@escritorio.com", "login:ip:203.0.113.10");
        assertThat(h.attempts.windows.values()).containsOnly(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("without a client address only the email is counted")
    void withoutAddress() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);

        login.login(loginAs(AuthHarness.PASSWORD, null, null));

        assertThat(h.attempts.registered).containsExactly("login:email:ana@escritorio.com");
        assertThat(h.attempts.reset).containsExactly("login:email:ana@escritorio.com");
    }

    @Test
    @DisplayName("the email allows 10 attempts and the address 30")
    void attemptLimits() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        h.attempts.counters.put("login:email:ana@escritorio.com", 9);
        h.attempts.counters.put("login:ip:203.0.113.10", 29);
        login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10"));

        h.attempts.counters.put("login:email:ana@escritorio.com", 10);
        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10")))
                .isInstanceOf(InvalidCredentialsException.class);
        h.attempts.counters.put("login:email:ana@escritorio.com", 0);
        h.attempts.counters.put("login:ip:203.0.113.10", 30);
        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("over the limit is refused before the password is looked at, but both counters still count")
    void overTheLimit() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        h.attempts.counters.put("login:email:ana@escritorio.com", 10);

        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(h.attempts.registered).containsExactly("login:email:ana@escritorio.com", "login:ip:203.0.113.10");
        assertThat(h.attempts.reset).isEmpty();
        assertThat(h.store.tokens).isEmpty();
    }

    @Test
    @DisplayName("a wrong password is refused and keeps the counters")
    void wrongPassword() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);

        assertThatThrownBy(() -> login.login(loginAs("wrong", null, "203.0.113.10")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(h.attempts.reset).isEmpty();
        assertThat(h.hasher.wastedTime).isZero();
        assertThat(h.store.tokens).isEmpty();
    }

    @Test
    @DisplayName("an unknown email still pays the cost of a password check")
    void unknownEmail() {
        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(h.hasher.wastedTime).isEqualTo(1);
    }

    @Test
    @DisplayName("a user with two factor must send the code, and a wrong one is refused")
    void twoFactor() {
        userWithTwoFactor();

        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, null, "203.0.113.10")))
                .isInstanceOf(TwoFactorRequiredException.class);
        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, "  ", "203.0.113.10")))
                .isInstanceOf(TwoFactorRequiredException.class);
        assertThatThrownBy(() -> login.login(loginAs(AuthHarness.PASSWORD, "000000", "203.0.113.10")))
                .isInstanceOf(InvalidTwoFactorCodeException.class);
        assertThat(h.attempts.reset).isEmpty();
        assertThat(h.store.tokens).isEmpty();

        assertThat(login.login(loginAs(AuthHarness.PASSWORD, Fakes.Totp.VALID_CODE, "203.0.113.10")).accessToken())
                .isNotBlank();
    }

    @Test
    @DisplayName("a user without two factor is not asked for a code")
    void noTwoFactor() {
        h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);

        assertThat(login.login(loginAs(AuthHarness.PASSWORD, "999999", null)).accessToken()).isNotBlank();
    }

    private String sessionFor(User user) {
        return h.sessions.start(user).refreshToken();
    }

    @Test
    @DisplayName("refresh rotates the token: the old one is revoked and the new one works")
    void refreshRotates() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        String first = sessionFor(user);

        SessionResponse next = refresh.refresh(new RefreshSessionCommand(first));

        assertThat(next.refreshToken()).isNotEqualTo(first);
        assertThat(next.user().id()).isEqualTo(user.id().value());
        assertThat(h.store.byHash(RefreshTokenSecret.hash(first)).orElseThrow().revokedAt()).isEqualTo(Fakes.NOW);
        assertThat(h.store.byHash(RefreshTokenSecret.hash(next.refreshToken())).orElseThrow().isRevoked()).isFalse();
        assertThat(refresh.refresh(new RefreshSessionCommand(next.refreshToken())).accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("refresh reflects the current data of the user")
    void refreshUsesCurrentUser() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADMIN);
        String token = sessionFor(user);
        User changed = h.store.byId(user.id()).orElseThrow();
        changed.changeRole(Role.SECRETARIA);
        h.store.save(changed);

        assertThat(refresh.refresh(new RefreshSessionCommand(token)).user().role()).isEqualTo(Role.SECRETARIA);
    }

    @Test
    @DisplayName("an unknown, missing or blank token is invalid")
    void refreshInvalid() {
        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand("unknown")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand(null)))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand("  ")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("an expired token is refused with its own error and is not rotated")
    void refreshExpired() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        String raw = RefreshTokenSecret.generate();
        h.store.save(RefreshToken.issue(user.id(), RefreshTokenSecret.hash(raw), Fakes.NOW));

        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand(raw)))
                .isInstanceOf(ExpiredRefreshTokenException.class);

        assertThat(h.store.byHash(RefreshTokenSecret.hash(raw)).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("presenting a revoked token again revokes every session of the user")
    void reuseRevokesEverything() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        String first = sessionFor(user);
        String second = refresh.refresh(new RefreshSessionCommand(first)).refreshToken();

        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand(first)))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(h.store.tokens.values()).allMatch(RefreshToken::isRevoked);
        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand(second)))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("refresh for a deleted user is invalid and the token stays revoked")
    void refreshForDeletedUser() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        String token = sessionFor(user);
        h.store.delete(user.organizationId(), user.id());

        assertThatThrownBy(() -> refresh.refresh(new RefreshSessionCommand(token)))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(h.store.byHash(RefreshTokenSecret.hash(token)).orElseThrow().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("logout revokes only the presented token")
    void logoutRevokesThePresentedToken() {
        User user = h.addUser(h.addOrganization("Escritorio").id(), "Ana", EMAIL.value(), Role.ADVOGADO);
        String first = sessionFor(user);
        String other = sessionFor(user);

        logout.logout(new LogoutCommand(first));

        assertThat(h.store.byHash(RefreshTokenSecret.hash(first)).orElseThrow().isRevoked()).isTrue();
        assertThat(h.store.byHash(RefreshTokenSecret.hash(other)).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("logout ignores a missing, blank or unknown token")
    void logoutIsForgiving() {
        logout.logout(new LogoutCommand(null));
        logout.logout(new LogoutCommand(" "));
        logout.logout(new LogoutCommand("unknown"));

        assertThat(h.store.tokens).isEmpty();
    }
}
