package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenSecret")
class RefreshTokenSecretTest {

    @Test
    @DisplayName("generated secrets are url-safe and different every time")
    void generatesDifferentSecrets() {
        String first = RefreshTokenSecret.generate();
        String second = RefreshTokenSecret.generate();

        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("the hash is a stable SHA-256 in hex and never the secret itself")
    void hashesWithSha256() {
        assertThat(RefreshTokenSecret.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(RefreshTokenSecret.hash("abc")).isEqualTo(RefreshTokenSecret.hash("abc"));
    }
}
