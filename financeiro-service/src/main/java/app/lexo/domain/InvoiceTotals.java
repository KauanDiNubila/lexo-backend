package app.lexo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

public record InvoiceTotals(BigDecimal paid, BigDecimal pending, BigDecimal overdue) {

    public static InvoiceTotals of(Collection<Invoice> invoices, Instant now) {
        return new InvoiceTotals(
                sum(invoices, InvoiceStatus.PAID, now),
                sum(invoices, InvoiceStatus.PENDING, now),
                sum(invoices, InvoiceStatus.OVERDUE, now));
    }

    private static BigDecimal sum(Collection<Invoice> invoices, InvoiceStatus status, Instant now) {
        return invoices.stream()
                .filter(invoice -> invoice.statusAt(now) == status)
                .map(invoice -> invoice.data().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
