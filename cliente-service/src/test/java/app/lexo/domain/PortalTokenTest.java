package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortalToken")
class PortalTokenTest {

    @Test
    @DisplayName("generates distinct 64 character hexadecimal tokens")
    void generate() {
        String first = PortalToken.generate();
        String second = PortalToken.generate();

        assertThat(first).matches("[0-9a-f]{64}");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("hash is the SHA-256 of the token in hexadecimal and is stable")
    void hash() {
        assertThat(PortalToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .isEqualTo(PortalToken.hash("abc"));
    }
}
