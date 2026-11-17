package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenSecret")
class TokenSecretTest {

    @Test
    @DisplayName("generated secrets are url-safe and different every time")
    void generatesDifferentSecrets() {
        String first = TokenSecret.generate();
        String second = TokenSecret.generate();

        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("the hash is a stable SHA-256 in hex and never the secret itself")
    void hashesWithSha256() {
        assertThat(TokenSecret.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(TokenSecret.hash("abc")).isEqualTo(TokenSecret.hash("abc"));
    }
}
