package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PortalSummary(@JsonProperty("totalProcessos") int totalCases, @JsonProperty("emAberto") BigDecimal outstanding) {
}
