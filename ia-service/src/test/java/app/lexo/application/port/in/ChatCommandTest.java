package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.ChatRole;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatCommand")
class ChatCommandTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    @Test
    @DisplayName("needs at least one message")
    void needsAMessage() {
        assertThatThrownBy(() -> new ChatCommand(ORGANIZATION, List.of(), Optional.empty()))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Envie ao menos uma mensagem");
    }

    @Test
    @DisplayName("keeps the messages in order")
    void keepsMessages() {
        List<ChatMessage> messages = List.of(
                new ChatMessage(ChatRole.USER, "Oi"), new ChatMessage(ChatRole.ASSISTANT, "Olá"));

        assertThat(new ChatCommand(ORGANIZATION, messages, Optional.empty()).messages()).isEqualTo(messages);
    }
}
