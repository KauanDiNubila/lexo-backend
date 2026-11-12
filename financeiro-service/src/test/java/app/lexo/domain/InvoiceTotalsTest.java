package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvoiceTotals")
class InvoiceTotalsTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("organization-1");

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    private Invoice invoice(String amount, InvoiceStatus status) {
        return invoice(amount, status, Instant.parse("2027-01-01T00:00:00Z"));
    }

    private Invoice invoice(String amount, InvoiceStatus status, Instant dueDate) {
        return Invoice.issue(ORGANIZATION, new InvoiceData(new ClientId("c"), null, "x", new BigDecimal(amount),
                status, dueDate), NOW);
    }

    @Test
    @DisplayName("sums by status and ignores CANCELLED")
    void sumsByStatus() {
        InvoiceTotals totals = InvoiceTotals.of(List.of(
                invoice("100.00", InvoiceStatus.PAID), invoice("50.00", InvoiceStatus.PAID),
                invoice("40.50", InvoiceStatus.PENDING), invoice("10.25", InvoiceStatus.OVERDUE),
                invoice("999.00", InvoiceStatus.CANCELLED)), NOW);

        assertThat(totals.paid()).isEqualByComparingTo("150.00");
        assertThat(totals.pending()).isEqualByComparingTo("40.50");
        assertThat(totals.overdue()).isEqualByComparingTo("10.25");
    }

    @Test
    @DisplayName("a PENDING invoice past its due date counts as overdue, not as pending")
    void pastDuePendingCountsAsOverdue() {
        Instant past = Instant.parse("2026-01-01T00:00:00Z");

        InvoiceTotals totals = InvoiceTotals.of(List.of(
                invoice("70.00", InvoiceStatus.PENDING, past), invoice("5.00", InvoiceStatus.PENDING),
                invoice("30.00", InvoiceStatus.PAID, past)), NOW);

        assertThat(totals.overdue()).isEqualByComparingTo("70.00");
        assertThat(totals.pending()).isEqualByComparingTo("5.00");
        assertThat(totals.paid()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("an empty list gives zeros, not nulls")
    void empty() {
        InvoiceTotals totals = InvoiceTotals.of(List.of(), NOW);

        assertThat(totals.paid()).isEqualByComparingTo("0");
        assertThat(totals.pending()).isEqualByComparingTo("0");
        assertThat(totals.overdue()).isEqualByComparingTo("0");
    }
}
