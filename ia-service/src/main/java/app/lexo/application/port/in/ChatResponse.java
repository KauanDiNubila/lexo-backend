package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResponse(
        @JsonProperty("resposta") String reply,
        @JsonProperty("fonte") String source
) {
}
