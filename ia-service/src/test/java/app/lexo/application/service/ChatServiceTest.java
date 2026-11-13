package app.lexo.application.service;

import app.lexo.Texts;
import app.lexo.application.port.in.ChatCommand;
import app.lexo.application.port.in.ChatResponse;
import app.lexo.application.service.Fakes.RateLimit;
import app.lexo.application.service.Fakes.Text;
import app.lexo.domain.CaseContext;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.ChatRole;
import app.lexo.domain.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static app.lexo.application.service.Fakes.ORGANIZATION;
import static app.lexo.application.service.Fakes.SETTINGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatService")
class ChatServiceTest {

    private final Text text = new Text();
    private final RateLimit rateLimit = new RateLimit();
    private final ChatService service = new ChatService(new UsageQuota(rateLimit, SETTINGS), text);

    private final List<ChatMessage> messages = List.of(
            new ChatMessage(ChatRole.USER, "Qual o prazo?"),
            new ChatMessage(ChatRole.ASSISTANT, "Quinze dias."));

    @Test
    @DisplayName("sends the conversation and the persona with the case context, and returns the trimmed answer")
    void usesTheModel() {
        text.answer = Optional.of(" Resposta \n");
        CaseContext context = new CaseContext("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado");

        ChatResponse response = service.chat(new ChatCommand(ORGANIZATION, messages, Optional.of(context)));

        assertThat(response.reply()).isEqualTo("Resposta");
        assertThat(response.source()).isEqualTo("gemini");
        assertThat(text.conversations).containsExactly(messages);
        assertThat(text.systemInstructions).containsExactly(Texts.PERSONA
                + "\n\nA conversa é sobre o seguinte processo (use como contexto):\n"
                + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado"));
    }

    @Test
    @DisplayName("without a case context it sends only the persona")
    void onlyThePersona() {
        text.answer = Optional.of("ok");

        service.chat(new ChatCommand(ORGANIZATION, messages, Optional.empty()));

        assertThat(text.systemInstructions).containsExactly(Texts.PERSONA);
    }

    @Test
    @DisplayName("a context without fields uses the default wording")
    void emptyContext() {
        text.answer = Optional.of("ok");

        service.chat(new ChatCommand(ORGANIZATION, messages, Optional.of(new CaseContext(null, null, null, null))));

        assertThat(text.systemInstructions.get(0)).endsWith(
                Texts.caseLines("não informado", "não informada", "não informado", "não informado"));
    }

    @Test
    @DisplayName("says the AI is unavailable when the model gives nothing")
    void unavailable() {
        ChatResponse response = service.chat(new ChatCommand(ORGANIZATION, messages, Optional.empty()));

        assertThat(response.source()).isEqualTo("mock");
        assertThat(response.reply()).isEqualTo(Texts.CHAT_UNAVAILABLE);
    }

    @Test
    @DisplayName("spends the organization limit before asking the model, and refuses past it")
    void limit() {
        service.chat(new ChatCommand(ORGANIZATION, messages, Optional.empty()));
        assertThat(rateLimit.calls).hasSize(1);

        rateLimit.allow = false;
        int callsBefore = text.calls();
        assertThatThrownBy(() -> service.chat(new ChatCommand(ORGANIZATION, messages, Optional.empty())))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(text.calls()).isEqualTo(callsBefore);
    }
}
