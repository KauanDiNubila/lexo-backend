package app.lexo.common.signing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionCutoff")
class SessionCutoffTest {

    @Test
    @DisplayName("the key is the same on both sides of the contract")
    void keyIsStable() {
        assertThat(SessionCutoff.key("user-1")).isEqualTo("session-cutoff:user-1");
    }
}
