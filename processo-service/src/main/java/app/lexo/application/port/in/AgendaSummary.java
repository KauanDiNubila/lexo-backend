package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AgendaSummary(
        @JsonProperty("urgentes") long urgent,
        @JsonProperty("vencidos") long overdue,
        @JsonProperty("proximos") List<DeadlineResponse> upcoming
) {
}
