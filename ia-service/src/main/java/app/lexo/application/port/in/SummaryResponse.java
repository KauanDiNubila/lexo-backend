package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryResponse(
        @JsonProperty("resumo") String summary,
        @JsonProperty("fonte") String source
) {
}
