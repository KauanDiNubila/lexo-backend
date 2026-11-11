package app.lexo.domain;

import java.math.BigDecimal;
import java.util.Collection;

public record InvoiceTotals(BigDecimal paid, BigDecimal pending, BigDecimal overdue) {

    public static InvoiceTotals of(Collection<Invoice> invoices) {
        return new InvoiceTotals(
                sum(invoices, InvoiceStatus.PAID),
                sum(invoices, InvoiceStatus.PENDING),
                sum(invoices, InvoiceStatus.OVERDUE));
    }

    private static BigDecimal sum(Collection<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream()
                .filter(invoice -> invoice.data().status() == status)
                .map(invoice -> invoice.data().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
