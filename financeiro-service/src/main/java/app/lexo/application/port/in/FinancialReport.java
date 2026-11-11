package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record FinancialReport(
        Instant start,
        Instant end,
        @JsonProperty("totalPago") BigDecimal totalPaid,
        @JsonProperty("totalPendente") BigDecimal totalPending,
        @JsonProperty("totalAtrasado") BigDecimal totalOverdue
) {
}
