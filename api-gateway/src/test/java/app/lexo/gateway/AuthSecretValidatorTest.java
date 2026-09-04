package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthSecretValidator")
class AuthSecretValidatorTest {

    private static final String SEGREDO_VALIDO = "auth-secret-de-producao-com-no-minimo-32-bytes";

    @Test
    @DisplayName("segredo valido e customizado nunca lanca, mesmo fora de dev")
    void segredoValidoNuncaLanca() {
        var validator = new AuthSecretValidator(SEGREDO_VALIDO, new MockEnvironment());

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("segredo default fora do profile dev/test recusa iniciar")
    void segredoDefaultForaDeDevRecusaIniciar() {
        var validator = new AuthSecretValidator(AuthSecretValidator.SEGREDO_DEV, new MockEnvironment());

        assertThatThrownBy(validator::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("segredo ausente ou em branco fora do profile dev/test recusa iniciar")
    void segredoAusenteForaDeDevRecusaIniciar() {
        var semSegredo = new AuthSecretValidator(null, new MockEnvironment());
        var segredoEmBranco = new AuthSecretValidator("   ", new MockEnvironment());

        assertThatThrownBy(semSegredo::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(segredoEmBranco::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("segredo default e tolerado no profile dev, so com aviso")
    void segredoDefaultNoProfileDevNaoLanca() {
        var env = new MockEnvironment();
        env.setActiveProfiles("dev");
        var validator = new AuthSecretValidator(AuthSecretValidator.SEGREDO_DEV, env);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("segredo default e tolerado no profile test")
    void segredoDefaultNoProfileTestNaoLanca() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");
        var validator = new AuthSecretValidator(AuthSecretValidator.SEGREDO_DEV, env);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }
}
