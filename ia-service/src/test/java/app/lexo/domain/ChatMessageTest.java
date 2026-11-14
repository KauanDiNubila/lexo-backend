package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Test
    @DisplayName("keeps the role and the text")
    void keepsValues() {
        ChatMessage message = new ChatMessage(ChatRole.ASSISTANT, "Quinze dias.");

        assertThat(message.role()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(message.text()).isEqualTo("Quinze dias.");
    }

    @Test
    @DisplayName("rejects a missing role and a null or blank text")
    void rejectsInvalid() {
        assertThatThrownBy(() -> new ChatMessage(null, "Oi")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatMessage(ChatRole.USER, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatMessage(ChatRole.USER, "  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
