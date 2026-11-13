package app.lexo.adapter.out.gemini;

import app.lexo.GeminiStub;
import app.lexo.GeminiStub.Received;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.ChatRole;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeminiTextGenerationAdapter")
class GeminiTextGenerationAdapterTest {

    private static final GeminiStub GEMINI = new GeminiStub();

    private final GeminiTextGenerationAdapter adapter =
            new GeminiTextGenerationAdapter("chave", "modelo-x", GEMINI.baseUrl(), 2, 5);

    @BeforeEach
    void reset() {
        GEMINI.reset();
    }

    @AfterAll
    static void stop() {
        GEMINI.close();
    }

    @Test
    @DisplayName("generate posts one prompt without role or system instruction and returns the first candidate text")
    void generate() {
        GEMINI.answers("texto gerado");

        Optional<String> result = adapter.generate("escreva");

        assertThat(result).contains("texto gerado");
        Received request = GEMINI.onlyRequest();
        assertThat(request.path()).isEqualTo("/v1beta/models/modelo-x:generateContent");
        assertThat(request.query()).isNull();
        assertThat(request.apiKey()).isEqualTo("chave");
        assertThat(request.body().has("system_instruction")).isFalse();
        assertThat(request.body().at("/contents/0/parts/0/text").asText()).isEqualTo("escreva");
        assertThat(request.body().at("/contents/0").has("role")).isFalse();
    }

    @Test
    @DisplayName("converse sends the system instruction and maps the assistant to the model role")
    void converse() {
        GEMINI.answers("ok");

        adapter.converse("seja breve", List.of(
                new ChatMessage(ChatRole.USER, "Oi"),
                new ChatMessage(ChatRole.ASSISTANT, "Olá")));

        Received request = GEMINI.onlyRequest();
        assertThat(request.body().at("/system_instruction/parts/0/text").asText()).isEqualTo("seja breve");
        assertThat(request.body().at("/contents/0/role").asText()).isEqualTo("user");
        assertThat(request.body().at("/contents/1/role").asText()).isEqualTo("model");
        assertThat(request.body().at("/contents/1/parts/0/text").asText()).isEqualTo("Olá");
    }

    @Test
    @DisplayName("a blank system instruction is not sent")
    void blankSystemInstruction() {
        GEMINI.answers("ok");

        adapter.converse(" ", List.of(new ChatMessage(ChatRole.USER, "Oi")));

        assertThat(GEMINI.onlyRequest().body().has("system_instruction")).isFalse();
    }

    @Test
    @DisplayName("gives nothing, without calling Gemini, when there is no key")
    void withoutKey() {
        GEMINI.answers("nao deveria ser usado");

        assertThat(new GeminiTextGenerationAdapter("", "m", GEMINI.baseUrl(), 2, 5).generate("x")).isEmpty();
        assertThat(new GeminiTextGenerationAdapter(" ", "m", GEMINI.baseUrl(), 2, 5)
                .converse("s", List.of(new ChatMessage(ChatRole.USER, "x")))).isEmpty();
        assertThat(GEMINI.requests()).isEmpty();
    }

    @Test
    @DisplayName("gives nothing when Gemini errors, answers without text or answers something that is not JSON")
    void failuresGiveNothing() {
        GEMINI.answersWith(500, "{\"error\":\"boom\"}");
        assertThat(adapter.generate("x")).isEmpty();

        GEMINI.answersWith(200, "{}");
        assertThat(adapter.generate("x")).isEmpty();

        GEMINI.answers("   ");
        assertThat(adapter.generate("x")).isEmpty();

        GEMINI.answersWith(200, "isto nao e json");
        assertThat(adapter.generate("x")).isEmpty();
    }

    @Test
    @DisplayName("gives nothing when Gemini cannot be reached")
    void unreachable() {
        GeminiTextGenerationAdapter unreachable =
                new GeminiTextGenerationAdapter("chave", "m", "http://127.0.0.1:1/v1beta", 1, 1);

        assertThat(unreachable.generate("x")).isEmpty();
    }

    @Test
    @DisplayName("never writes the API key to the log when the call fails")
    void keyStaysOutOfTheLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(GeminiTextGenerationAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new GeminiTextGenerationAdapter("segredo-da-chave", "m", "http://127.0.0.1:1/v1beta", 1, 1)
                    .generate("x");
            GEMINI.answersWith(500, "{}");
            adapter.generate("x");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains("segredo-da-chave"));
    }
}
