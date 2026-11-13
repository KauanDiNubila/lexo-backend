package app.lexo.adapter.out.gemini;

import app.lexo.application.port.out.TextGenerationPort;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.ChatRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
class GeminiTextGenerationAdapter implements TextGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextGenerationAdapter.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient http;

    GeminiTextGenerationAdapter(
            @Value("${lexo.ia.gemini.api-key}") String apiKey,
            @Value("${lexo.ia.gemini.model}") String model,
            @Value("${lexo.ia.gemini.base-url}") String baseUrl,
            @Value("${lexo.ia.gemini.connect-timeout-seconds}") long connectTimeout,
            @Value("${lexo.ia.gemini.read-timeout-seconds}") long readTimeout) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<String> generate(String prompt) {
        List<Map<String, Object>> contents = List.of(
                Map.of("parts", List.of(Map.of("text", prompt))));
        return call(null, contents);
    }

    @Override
    public Optional<String> converse(String systemInstruction, List<ChatMessage> messages) {
        List<Map<String, Object>> contents = messages.stream().map(GeminiTextGenerationAdapter::content).toList();
        return call(systemInstruction, contents);
    }

    private static Map<String, Object> content(ChatMessage message) {
        String role = message.role() == ChatRole.ASSISTANT ? "model" : "user";
        return Map.of("role", role, "parts", List.of(Map.of("text", message.text())));
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private Optional<String> call(String systemInstruction, List<Map<String, Object>> contents) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("contents", contents);
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
            }
            JsonNode response = http.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = response == null ? null
                    : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            return Optional.ofNullable(text).filter(t -> !t.isBlank());
        } catch (Exception e) {
            log.warn("Gemini call failed, using the fallback: {}", e.toString());
            return Optional.empty();
        }
    }
}
