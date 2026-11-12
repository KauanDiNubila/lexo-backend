package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CaseSummary(
        long total,
        @JsonProperty("ativos") long active,
        @JsonProperty("porStatus") Map<String, Long> byStatus
) {
}
