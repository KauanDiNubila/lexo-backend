package app.lexo.config;

import app.lexo.common.signing.GatewaySignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Secrets validator")
class SecretsValidatorTest {

    private static final String SECRET = "um-segredo-customizado-com-bem-mais-de-32-bytes";
    private static final String KEY = "chave-de-assinatura-customizada-com-mais-de-32-bytes";

    private static MockEnvironment profile(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    private static void validate(String secret, String key, MockEnvironment env) {
        new SecretsValidator(secret, key, env).afterPropertiesSet();
    }

    @Test
    @DisplayName("custom secrets never fail, even outside dev")
    void customSecrets() {
        assertThatCode(() -> validate(SECRET, KEY, profile())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the default auth secret outside dev refuses to start")
    void defaultAuthSecret() {
        assertThatThrownBy(() -> validate(SecretsValidator.DEV_AUTH_SECRET, KEY, profile()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("AUTH_SECRET");
    }

    @Test
    @DisplayName("a missing or blank auth secret outside dev refuses to start")
    void missingAuthSecret() {
        assertThatThrownBy(() -> validate(null, KEY, profile())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> validate("  ", KEY, profile())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("the default auth secret is tolerated in the dev and test profiles")
    void defaultAuthSecretInDev() {
        assertThatCode(() -> validate(SecretsValidator.DEV_AUTH_SECRET, KEY, profile("dev")))
                .doesNotThrowAnyException();
        assertThatCode(() -> validate(SecretsValidator.DEV_AUTH_SECRET, KEY, profile("test")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the default signing key outside dev refuses to start")
    void defaultSigningKey() {
        assertThatThrownBy(() -> validate(SECRET, GatewaySignature.CHAVE_DEV, profile()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("GATEWAY_SIGNING_KEY");
        assertThatCode(() -> validate(SECRET, GatewaySignature.CHAVE_DEV, profile("dev"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a missing or short signing key refuses to start in any profile")
    void weakSigningKey() {
        assertThatThrownBy(() -> validate(SECRET, "", profile("dev"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> validate(SECRET, "curta", profile("dev"))).isInstanceOf(IllegalStateException.class);
    }
}
