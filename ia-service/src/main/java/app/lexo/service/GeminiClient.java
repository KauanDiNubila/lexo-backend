package app.lexo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient http = RestClient.create();

    public GeminiClient(
            @Value("${lexo.ia.gemini.api-key}") String apiKey,
            @Value("${lexo.ia.gemini.model}") String model,
            @Value("${lexo.ia.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> gerar(String prompt) {
        List<Map<String, Object>> contents = List.of(
                Map.of("parts", List.of(Map.of("text", prompt))));
        return chamar(null, contents);
    }

    public Optional<String> conversar(String systemInstruction, List<Map<String, Object>> contents) {
        return chamar(systemInstruction, contents);
    }

    private Optional<String> chamar(String systemInstruction, List<Map<String, Object>> contents) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("contents", contents);
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
            }
            JsonNode resp = http.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String texto = resp == null ? null
                    : resp.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            return Optional.ofNullable(texto).filter(t -> !t.isBlank());
        } catch (Exception e) {
            log.warn("Chamada ao Gemini falhou, usando fallback: {}", e.toString());
            return Optional.empty();
        }
    }
}
