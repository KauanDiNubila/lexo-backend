package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deadline")
class DeadlineTest {

    @Test
    @DisplayName("a pending deadline, or one with no status, is open")
    void open() {
        assertThat(new Deadline("a", "d", "PENDENTE").isOpen()).isTrue();
        assertThat(new Deadline("a", "d", null).isOpen()).isTrue();
        assertThat(new Deadline("a", "d", " ").isOpen()).isTrue();
        assertThat(new Deadline("a", "d", "outro").isOpen()).isTrue();
    }

    @Test
    @DisplayName("a completed or missed deadline is closed, whatever the case")
    void closed() {
        assertThat(new Deadline("a", "d", "CONCLUIDO").isOpen()).isFalse();
        assertThat(new Deadline("a", "d", "perdido").isOpen()).isFalse();
        assertThat(new Deadline("a", "d", " Concluido ").isOpen()).isFalse();
    }
}
