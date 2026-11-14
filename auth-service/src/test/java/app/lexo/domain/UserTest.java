package app.lexo.domain;

import app.lexo.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User")
class UserTest {

    private User newUser() {
        return User.register(new OrganizationId("org-1"), "Ana", new EmailAddress("ana@escritorio.com"), "hash",
                Role.ADVOGADO);
    }

    @Test
    @DisplayName("a new user has no id and no two factor")
    void startsWithoutTwoFactor() {
        User user = newUser();

        assertThat(user.id()).isNull();
        assertThat(user.twoFactorEnabled()).isFalse();
        assertThat(user.requiresTwoFactor()).isFalse();
        assertThat(user.hasTwoFactorSecret()).isFalse();
        assertThat(user.pendingTwoFactorSecret()).isEmpty();
    }

    @Test
    @DisplayName("setup keeps the secret pending until it is confirmed")
    void setupIsPendingUntilConfirmed() {
        User user = newUser();

        user.beginTwoFactorSetup("enc-1");

        assertThat(user.pendingTwoFactorSecret()).contains("enc-1");
        assertThat(user.requiresTwoFactor()).isFalse();
        assertThat(user.twoFactorSecret()).isNull();
    }

    @Test
    @DisplayName("confirming promotes the pending secret and turns two factor on")
    void confirmPromotesTheSecret() {
        User user = newUser();
        user.beginTwoFactorSetup("enc-1");

        user.confirmTwoFactorSetup();

        assertThat(user.twoFactorSecret()).isEqualTo("enc-1");
        assertThat(user.twoFactorEnabled()).isTrue();
        assertThat(user.requiresTwoFactor()).isTrue();
        assertThat(user.pendingTwoFactorSecret()).isEmpty();
    }

    @Test
    @DisplayName("confirming without a pending setup is a programming error")
    void confirmNeedsPendingSetup() {
        assertThatThrownBy(() -> newUser().confirmTwoFactorSetup()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("disabling clears the secret, the flag and any pending setup")
    void disableClearsEverything() {
        User user = newUser();
        user.beginTwoFactorSetup("enc-1");
        user.confirmTwoFactorSetup();
        user.beginTwoFactorSetup("enc-2");

        user.disableTwoFactor();

        assertThat(user.twoFactorSecret()).isNull();
        assertThat(user.twoFactorEnabled()).isFalse();
        assertThat(user.pendingTwoFactorSecret()).isEmpty();
    }

    @Test
    @DisplayName("two factor only counts when it is enabled and has a secret")
    void requiresBothFlagAndSecret() {
        User flagOnly = User.restore(new UserId("u1"), new OrganizationId("org-1"), "Ana",
                new EmailAddress("ana@escritorio.com"), "hash", Role.ADMIN, null, true, null, null);
        User secretOnly = User.restore(new UserId("u2"), new OrganizationId("org-1"), "Ana",
                new EmailAddress("ana2@escritorio.com"), "hash", Role.ADMIN, "enc", false, null, null);

        assertThat(flagOnly.requiresTwoFactor()).isFalse();
        assertThat(secretOnly.requiresTwoFactor()).isFalse();
        assertThat(secretOnly.hasTwoFactorSecret()).isTrue();
    }

    @Test
    @DisplayName("rename trims the name and the role can change")
    void renameAndChangeRole() {
        User user = newUser();

        user.rename("  Ana Maria ");
        user.changeRole(Role.SECRETARIA);

        assertThat(user.name()).isEqualTo("Ana Maria");
        assertThat(user.role()).isEqualTo(Role.SECRETARIA);
    }
}
