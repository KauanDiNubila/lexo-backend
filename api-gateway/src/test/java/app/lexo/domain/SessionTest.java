package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Session and identity")
class SessionTest {

    private final Identity identity = new Identity("u-1", "org-1", "ADMIN", "Ana", "ana@x.com");

    @Test
    @DisplayName("a session issued at or before the cutoff has ended")
    void endedAtOrBeforeTheCutoff() {
        assertThat(new Session(identity, 100).isEndedBy(100)).isTrue();
        assertThat(new Session(identity, 99).isEndedBy(100)).isTrue();
    }

    @Test
    @DisplayName("a session issued after the cutoff is still valid")
    void validAfterTheCutoff() {
        assertThat(new Session(identity, 101).isEndedBy(100)).isFalse();
    }

    @Test
    @DisplayName("a session without an issue time (zero) ends under any cutoff")
    void noIssueTime() {
        assertThat(new Session(identity, 0).isEndedBy(1)).isTrue();
    }

    @Test
    @DisplayName("a missing name or e-mail becomes empty text")
    void missingNameAndEmail() {
        Identity bare = new Identity("u-1", "org-1", "ADMIN", null, null);

        assertThat(bare.name()).isEmpty();
        assertThat(bare.email()).isEmpty();
    }
}
