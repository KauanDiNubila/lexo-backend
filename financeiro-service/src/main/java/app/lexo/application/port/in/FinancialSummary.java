package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record FinancialSummary(
        @JsonProperty("emAberto") BigDecimal outstanding,
        @JsonProperty("recebido") BigDecimal received
) {
}
