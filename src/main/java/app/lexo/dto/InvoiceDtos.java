package app.lexo.dto;

import app.lexo.domain.Invoice;
import app.lexo.domain.enums.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record InvoiceRequest(
            @NotBlank(message = "Selecione um cliente") String clientId,
            String caseId,
            @NotBlank(message = "Descrição é obrigatória") String description,
            @NotNull @Positive(message = "Valor deve ser maior que zero") BigDecimal amount,
            @NotNull InvoiceStatus status,
            @NotNull(message = "Vencimento é obrigatório") Instant dueDate
    ) {
    }

    public record StatusRequest(
            @NotNull InvoiceStatus status
    ) {
    }

    public record InvoiceResponse(
            String id,
            String clientId,
            String caseId,
            String description,
            BigDecimal amount,
            InvoiceStatus status,
            Instant dueDate,
            Instant paidAt
    ) {
        public static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(
                    i.getId(), i.getClientId(), i.getCaseId(), i.getDescription(),
                    i.getAmount(), i.getStatus(), i.getDueDate(), i.getPaidAt());
        }
    }

    /** Resumo do relatorio financeiro por periodo. */
    public record FinancialReport(
            Instant start,
            Instant end,
            BigDecimal totalPago,
            BigDecimal totalPendente,
            BigDecimal totalAtrasado,
            List<InvoiceResponse> invoices
    ) {
    }
}
