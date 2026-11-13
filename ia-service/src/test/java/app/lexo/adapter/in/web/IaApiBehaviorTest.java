package app.lexo.adapter.in.web;

import app.lexo.GeminiStub.Received;
import app.lexo.IntegrationTestBase;
import app.lexo.Texts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AI API: behavior pinned before the hexagonal migration")
class IaApiBehaviorTest extends IntegrationTestBase {

    private static final String SUMMARIZE = "/api/ia/resumir-processo";
    private static final String CHAT = "/api/ia/chat";
    private static final String DRAFT = "/api/ia/rascunhar-peticao";

    private Map<String, Object> deadline(String title, String date, String status) {
        Map<String, Object> d = new HashMap<>();
        d.put("titulo", title);
        d.put("data", date);
        d.put("status", status);
        return d;
    }

    private Map<String, Object> fullSummaryRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("numero", "0001234-56.2024.8.26.0100");
        body.put("area", "Cível");
        body.put("status", "ATIVO");
        body.put("cliente", "Helena Prado");
        body.put("prazos", List.of(deadline("Contestação", "2026-12-01", "PENDENTE"),
                deadline("Audiência", "2026-12-10", "PENDENTE")));
        return body;
    }

    private Map<String, Object> context() {
        return Map.of("numero", "0001234-56.2024.8.26.0100", "area", "Cível", "status", "ATIVO",
                "cliente", "Helena Prado");
    }

    private Map<String, Object> message(String role, String text) {
        Map<String, Object> m = new HashMap<>();
        m.put("papel", role);
        m.put("texto", text);
        return m;
    }

    private String promptSentToGemini() {
        Received request = GEMINI.onlyRequest();
        return request.body().path("contents").path(0).path("parts").path(0).path("text").asText();
    }

    @Test
    @DisplayName("summarize sends the prompt to Gemini with the key in the query and returns the trimmed answer")
    void summarizeWithGemini() throws Exception {
        GEMINI.answers("  O processo segue em andamento.  \n");

        postAs(Identity.create(), SUMMARIZE, fullSummaryRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo").value("O processo segue em andamento."))
                .andExpect(jsonPath("$.fonte").value("gemini"));

        Received request = GEMINI.onlyRequest();
        assertThat(request.path()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(request.query()).isEqualTo("key=test-key");
        assertThat(request.body().has("system_instruction")).isFalse();
        assertThat(request.body().path("contents")).hasSize(1);
        assertThat(promptSentToGemini()).isEqualTo(Texts.SUMMARY_INTRO
                + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado") + "\n"
                + "Prazos:\n"
                + "  - Contestação (2026-12-01, PENDENTE)\n"
                + "  - Audiência (2026-12-10, PENDENTE)\n");
    }

    @Test
    @DisplayName("summarize fills missing fields with the default wording and says there are no deadlines")
    void summarizeDefaults() throws Exception {
        GEMINI.answers("ok");

        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(status().isOk());

        assertThat(promptSentToGemini()).isEqualTo(Texts.SUMMARY_INTRO
                + Texts.caseLines("não informado", "não informada", "não informado", "não informado") + "\n"
                + "Prazos: nenhum cadastrado.\n");
    }

    @Test
    @DisplayName("summarize names an incomplete deadline with the default wording")
    void summarizeIncompleteDeadline() throws Exception {
        GEMINI.answers("ok");
        Map<String, Object> body = new HashMap<>();
        body.put("prazos", List.of(deadline(null, "", null)));

        postAs(Identity.create(), SUMMARIZE, body).andExpect(status().isOk());

        assertThat(promptSentToGemini()).endsWith("Prazos:\n  - prazo (sem data, ?)\n");
    }

    @Test
    @DisplayName("summarize falls back to the automatic summary when Gemini fails")
    void summarizeFallsBackWhenGeminiFails() throws Exception {
        GEMINI.answersWith(500, "{\"error\":\"boom\"}");

        postAs(Identity.create(), SUMMARIZE, fullSummaryRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.resumo").value(
                        "O processo 0001234-56.2024.8.26.0100 (Cível) do cliente Helena Prado encontra-se ativo. "
                                + "Há 2 prazo(s) em aberto; o próximo é \"Contestação\" em 2026-12-01."
                                + Texts.SUMMARY_MOCK_TAIL));
    }

    @Test
    @DisplayName("the automatic summary uses the default wording and omits the date of a deadline without one")
    void automaticSummaryDefaults() throws Exception {
        GEMINI.answersWith(500, "");

        postAs(Identity.create(), SUMMARIZE, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.resumo").value(
                        "O processo sem número do cliente não informado encontra-se em andamento. "
                                + "Não há prazos cadastrados no momento." + Texts.SUMMARY_MOCK_TAIL));

        Map<String, Object> body = new HashMap<>();
        body.put("numero", "123");
        body.put("area", " ");
        body.put("prazos", List.of(deadline(null, null, "PENDENTE")));
        postAs(Identity.create(), SUMMARIZE, body)
                .andExpect(jsonPath("$.resumo").value(
                        "O processo 123 do cliente não informado encontra-se em andamento. "
                                + "Há 1 prazo(s) em aberto; o próximo é \"prazo\"." + Texts.SUMMARY_MOCK_TAIL));
    }

    @Test
    @DisplayName("an empty or malformed Gemini answer also falls back")
    void emptyGeminiAnswerFallsBack() throws Exception {
        GEMINI.answers("   ");
        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(jsonPath("$.fonte").value("mock"));

        GEMINI.answersWith(200, "{}");
        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(jsonPath("$.fonte").value("mock"));

        GEMINI.answersWith(200, "isto nao e json");
        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(jsonPath("$.fonte").value("mock"));
    }

    @Test
    @DisplayName("chat maps the roles to Gemini, skips blank messages and sends the persona with the case context")
    void chatWithContext() throws Exception {
        GEMINI.answers(" Resposta da IA \n");
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("user", "Qual o prazo?"));
        messages.add(message("assistant", "Quinze dias."));
        messages.add(message("user", "   "));
        messages.add(message("user", null));
        messages.add(message("outro", "Obrigada"));
        messages.add(message(null, "Mais uma"));

        postAs(Identity.create(), CHAT, Map.of("mensagens", messages, "contexto", context()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Resposta da IA"))
                .andExpect(jsonPath("$.fonte").value("gemini"));

        Received request = GEMINI.onlyRequest();
        assertThat(request.path()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(request.query()).isEqualTo("key=test-key");
        assertThat(request.body().path("system_instruction").path("parts").path(0).path("text").asText())
                .isEqualTo(Texts.PERSONA + "\n\nA conversa é sobre o seguinte processo (use como contexto):\n"
                        + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado"));
        assertThat(request.body().path("contents")).hasSize(4);
        assertThat(request.body().at("/contents/0/role").asText()).isEqualTo("user");
        assertThat(request.body().at("/contents/0/parts/0/text").asText()).isEqualTo("Qual o prazo?");
        assertThat(request.body().at("/contents/1/role").asText()).isEqualTo("model");
        assertThat(request.body().at("/contents/1/parts/0/text").asText()).isEqualTo("Quinze dias.");
        assertThat(request.body().at("/contents/2/role").asText()).isEqualTo("user");
        assertThat(request.body().at("/contents/2/parts/0/text").asText()).isEqualTo("Obrigada");
        assertThat(request.body().at("/contents/3/role").asText()).isEqualTo("user");
        assertThat(request.body().at("/contents/3/parts/0/text").asText()).isEqualTo("Mais uma");
    }

    @Test
    @DisplayName("chat without a case context sends only the persona, and a context without fields uses the default wording")
    void chatWithoutContext() throws Exception {
        GEMINI.answers("ok");
        postAs(Identity.create(), CHAT, Map.of("mensagens", List.of(message("user", "Oi"))))
                .andExpect(status().isOk());
        assertThat(GEMINI.onlyRequest().body().path("system_instruction").path("parts").path(0).path("text").asText())
                .isEqualTo(Texts.PERSONA);

        GEMINI.reset();
        GEMINI.answers("ok");
        postAs(Identity.create(), CHAT, Map.of("mensagens", List.of(message("user", "Oi")), "contexto", Map.of()))
                .andExpect(status().isOk());
        assertThat(GEMINI.onlyRequest().body().path("system_instruction").path("parts").path(0).path("text").asText())
                .isEqualTo(Texts.PERSONA + "\n\nA conversa é sobre o seguinte processo (use como contexto):\n"
                        + Texts.caseLines("não informado", "não informada", "não informado", "não informado"));
    }

    @Test
    @DisplayName("chat answers that the AI is unavailable when Gemini fails")
    void chatFallsBack() throws Exception {
        GEMINI.answersWith(503, "{}");

        postAs(Identity.create(), CHAT, Map.of("mensagens", List.of(message("user", "Oi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.resposta").value(Texts.CHAT_UNAVAILABLE));
    }

    @Test
    @DisplayName("chat with only blank messages still calls Gemini, with no conversation turns")
    void chatWithOnlyBlankMessages() throws Exception {
        GEMINI.answers("ok");

        postAs(Identity.create(), CHAT, Map.of("mensagens", List.of(message("user", " "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("gemini"));

        assertThat(GEMINI.onlyRequest().body().path("contents")).isEmpty();
    }

    @Test
    @DisplayName("draft sends the instruction, the request and the case context to Gemini and returns the trimmed text")
    void draftWithGemini() throws Exception {
        GEMINI.answers("\n MINUTA DA PETIÇÃO \n");

        postAs(Identity.create(), DRAFT, Map.of("pedido", "Contestação trabalhista", "contexto", context()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("MINUTA DA PETIÇÃO"))
                .andExpect(jsonPath("$.fonte").value("gemini"));

        assertThat(promptSentToGemini()).isEqualTo(Texts.PETITION_INTRO
                + "Pedido: Contestação trabalhista\n"
                + "\nContexto do processo:\n"
                + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado") + "\n");
    }

    @Test
    @DisplayName("draft without a case context ends the prompt after the request")
    void draftWithoutContext() throws Exception {
        GEMINI.answers("ok");

        postAs(Identity.create(), DRAFT, Map.of("pedido", "Contestação trabalhista")).andExpect(status().isOk());

        assertThat(promptSentToGemini()).isEqualTo(Texts.PETITION_INTRO + "Pedido: Contestação trabalhista\n");
    }

    @Test
    @DisplayName("draft falls back to the template when Gemini fails")
    void draftFallsBack() throws Exception {
        GEMINI.answersWith(500, "{}");

        postAs(Identity.create(), DRAFT, Map.of("pedido", "Contestação trabalhista"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.texto").value(Texts.petitionTemplate("Contestação trabalhista")));
    }

    @Test
    @DisplayName("invalid requests answer 400 VALIDACAO with the field message, and do not spend the limit or call Gemini")
    void validation() throws Exception {
        Identity id = Identity.create();

        postAs(id, CHAT, Map.of("mensagens", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Envie ao menos uma mensagem"));
        postAs(id, CHAT, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Envie ao menos uma mensagem"));
        postAs(id, CHAT, Map.of("mensagens", Collections.nCopies(31, message("user", "Oi"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Conversa muito longa"));
        postAs(id, CHAT, Map.of("mensagens", List.of(message("user", "a".repeat(8001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mensagem muito longa"));
        postAs(id, CHAT, Map.of("mensagens", List.of(message("p".repeat(21), "Oi"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Papel inválido"));
        postAs(id, SUMMARIZE, Map.of("prazos", Collections.nCopies(101, deadline("a", "b", "c"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Prazos demais para resumir"));
        postAs(id, SUMMARIZE, Map.of("numero", "n".repeat(61)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
        postAs(id, CHAT, Map.of("mensagens", List.of(message("user", "Oi")), "contexto", Map.of("cliente", "c".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
        postAs(id, DRAFT, Map.of("pedido", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Descreva o que deve ser redigido"));
        postAs(id, DRAFT, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Descreva o que deve ser redigido"));
        postAs(id, DRAFT, Map.of("pedido", "p".repeat(4001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pedido muito longo"));

        assertThat(GEMINI.requests()).isEmpty();
        verifyNoInteractions(redis);
    }

    @Test
    @DisplayName("a body that is not JSON answers 400 REQUISICAO_INVALIDA")
    void malformedBody() throws Exception {
        mvc.perform(post(CHAT)
                        .headers(asGateway(Identity.create()))
                        .contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Corpo da requisição inválido"));
    }

    @Test
    @DisplayName("without gateway headers the answer is 403, and with a wrong signature too")
    void unauthenticated() throws Exception {
        mvc.perform(post(SUMMARIZE)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        HttpHeaders forged = asGateway(Identity.create());
        forged.set("X-User-Role", "ADMIN");
        mvc.perform(post(SUMMARIZE)
                        .headers(forged)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        assertThat(GEMINI.requests()).isEmpty();
        verifyNoInteractions(redis);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ADVOGADO", "SECRETARIA"})
    @DisplayName("every role can use the AI")
    void anyRole(String role) throws Exception {
        GEMINI.answers("ok");

        postAs(Identity.create(role), SUMMARIZE, Map.of()).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {SUMMARIZE, CHAT, DRAFT})
    @DisplayName("each endpoint spends one unit of the organization limit, in the ratelimit:ia key with a 60 second window")
    void spendsTheLimit(String url) throws Exception {
        Identity id = Identity.create();
        GEMINI.answers("ok");

        postAs(id, url, bodyFor(url)).andExpect(status().isOk());

        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:ia:" + id.orgId())),
                eq("60"));
    }

    @ParameterizedTest
    @ValueSource(strings = {SUMMARIZE, CHAT, DRAFT})
    @DisplayName("past the limit the answer is 429 REQUISICOES_EM_EXCESSO and Gemini is not called")
    void overTheLimit(String url) throws Exception {
        redisCounts(4L);

        postAs(Identity.create(), url, bodyFor(url))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("REQUISICOES_EM_EXCESSO"))
                .andExpect(jsonPath("$.error").value("Muitas requisições de IA. Tente novamente em instantes."));

        assertThat(GEMINI.requests()).isEmpty();
    }

    @Test
    @DisplayName("the request that reaches the limit still passes, and an empty counter also passes")
    void limitBoundary() throws Exception {
        GEMINI.answers("ok");

        redisCounts(3L);
        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(status().isOk());

        redisCounts(null);
        postAs(Identity.create(), SUMMARIZE, Map.of()).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {SUMMARIZE, CHAT, DRAFT})
    @DisplayName("with Redis down the answer is 503 SERVICO_INDISPONIVEL and Gemini is not called")
    void redisDown(String url) throws Exception {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenThrow(new RuntimeException("redis fora do ar"));

        postAs(Identity.create(), url, bodyFor(url))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"))
                .andExpect(jsonPath("$.error").value("Lexo IA temporariamente indisponível. Tente novamente em instantes."));

        assertThat(GEMINI.requests()).isEmpty();
    }

    private Object bodyFor(String url) {
        return switch (url) {
            case CHAT -> Map.of("mensagens", List.of(message("user", "Oi")));
            case DRAFT -> Map.of("pedido", "Contestação");
            default -> Map.of();
        };
    }
}
