package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionResponse")
class SessionResponseTest {

    @Test
    @DisplayName("printing a session never shows the tokens")
    void toStringHidesTokens() {
        SessionResponse session = new SessionResponse("access-token-value",
                new UserInfo("u1", "Ana", "ana@escritorio.com", Role.ADMIN, "org-1"), "refresh-token-value");

        assertThat(session.toString()).doesNotContain("access-token-value").doesNotContain("refresh-token-value")
                .contains("ana@escritorio.com");
    }
}
