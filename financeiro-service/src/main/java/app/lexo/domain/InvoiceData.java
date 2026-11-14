package app.lexo.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceData(ClientId clientId, CaseId caseId, String description, BigDecimal amount,
                          InvoiceStatus status, Instant dueDate) {

    public InvoiceData withStatus(InvoiceStatus newStatus) {
        return new InvoiceData(clientId, caseId, description, amount, newStatus, dueDate);
    }
}
