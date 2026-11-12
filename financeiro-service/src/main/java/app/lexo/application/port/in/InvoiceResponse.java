package app.lexo.application.port.in;

import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;

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

    public static InvoiceResponse of(Invoice invoice, Instant now) {
        InvoiceData data = invoice.data();
        return new InvoiceResponse(
                invoice.id().value(),
                data.clientId().value(),
                data.caseId() == null ? null : data.caseId().value(),
                data.description(),
                data.amount(),
                invoice.statusAt(now),
                data.dueDate(),
                invoice.paidAt());
    }
}
