package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Session and identity")
class SessionTest {

    private final Identity identity = new Identity("u-1", "org-1", "ADMIN", "Ana", "ana@x.com");

    @Test
    @DisplayName("a session issued at or before the cutoff has ended")
    void endedAtOrBeforeTheCutoff() {
        assertThat(new Session(identity, OptionalLong.of(100)).isEndedBy(100)).isTrue();
        assertThat(new Session(identity, OptionalLong.of(99)).isEndedBy(100)).isTrue();
    }

    @Test
    @DisplayName("a session issued after the cutoff is still valid")
    void validAfterTheCutoff() {
        assertThat(new Session(identity, OptionalLong.of(101)).isEndedBy(100)).isFalse();
    }

    @Test
    @DisplayName("a session without an issue time ends under any cutoff")
    void noIssueTime() {
        assertThat(new Session(identity, OptionalLong.empty()).isEndedBy(1)).isTrue();
    }

    @Test
    @DisplayName("an identity needs a user, an organization and a role")
    void requiredFields() {
        assertThatThrownBy(() -> new Identity(null, "org-1", "ADMIN", "Ana", "a@x.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Identity("u-1", " ", "ADMIN", "Ana", "a@x.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Identity("u-1", "org-1", "", "Ana", "a@x.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a missing name or e-mail becomes empty text")
    void missingNameAndEmail() {
        Identity bare = new Identity("u-1", "org-1", "ADMIN", null, null);

        assertThat(bare.name()).isEmpty();
        assertThat(bare.email()).isEmpty();
    }
}
