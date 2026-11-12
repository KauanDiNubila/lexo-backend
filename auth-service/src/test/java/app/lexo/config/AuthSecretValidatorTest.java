package app.lexo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthSecretValidator")
class AuthSecretValidatorTest {

    private AuthSecretValidator validator(String secret, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return new AuthSecretValidator(secret, env);
    }

    @Test
    @DisplayName("the default secret is refused outside development")
    void refusesTheDefaultSecret() {
        assertThatThrownBy(() -> validator(AuthSecretValidator.DEFAULT_SECRET).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("AUTH_SECRET");
        assertThatThrownBy(() -> validator(AuthSecretValidator.DEFAULT_SECRET, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a blank or missing secret is refused outside development")
    void refusesABlankSecret() {
        assertThatThrownBy(() -> validator(" ").afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> validator(null).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("development and test profiles may use the default secret")
    void developmentMayUseTheDefault() {
        assertThatCode(() -> validator(AuthSecretValidator.DEFAULT_SECRET, "dev").afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatCode(() -> validator(AuthSecretValidator.DEFAULT_SECRET, "test").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a real secret is accepted anywhere")
    void acceptsARealSecret() {
        assertThatCode(() -> validator("a-real-secret-with-more-than-32-bytes!!").afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
