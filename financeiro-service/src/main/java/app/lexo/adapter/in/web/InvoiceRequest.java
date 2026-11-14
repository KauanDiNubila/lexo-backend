package app.lexo.adapter.in.web;

import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

record InvoiceRequest(
        @NotBlank(message = "Selecione um cliente") String clientId,
        String caseId,
        @NotBlank(message = "Descrição é obrigatória") String description,
        @NotNull @Positive(message = "Valor deve ser maior que zero") BigDecimal amount,
        @NotNull InvoiceStatus status,
        @NotNull(message = "Vencimento é obrigatório") Instant dueDate
) {

    InvoiceData toData() {
        return new InvoiceData(new ClientId(clientId), CaseId.optional(caseId), description, amount, status, dueDate);
    }
}
