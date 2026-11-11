package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Invoice")
class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-11T13:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("organization-1");

    private InvoiceData data(InvoiceStatus status) {
        return new InvoiceData(new ClientId("client-1"), null, "Consultation", new BigDecimal("100.00"), status,
                Instant.parse("2027-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("issuing as PAID stamps paidAt, any other status leaves it empty")
    void issue() {
        assertThat(Invoice.issue(ORGANIZATION, data(InvoiceStatus.PAID), NOW).paidAt()).isEqualTo(NOW);
        assertThat(Invoice.issue(ORGANIZATION, data(InvoiceStatus.PENDING), NOW).paidAt()).isNull();
        assertThat(Invoice.issue(ORGANIZATION, data(InvoiceStatus.OVERDUE), NOW).paidAt()).isNull();
        assertThat(Invoice.issue(ORGANIZATION, data(InvoiceStatus.PENDING), NOW).id()).isNull();
    }

    @Test
    @DisplayName("today, updating a PAID invoice stamps paidAt again")
    void updateRestampsPaidAt() {
        Invoice invoice = Invoice.issue(ORGANIZATION, data(InvoiceStatus.PAID), NOW);

        invoice.update(data(InvoiceStatus.PAID), LATER);

        assertThat(invoice.paidAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("updating to a status other than PAID clears paidAt")
    void updateClearsPaidAt() {
        Invoice invoice = Invoice.issue(ORGANIZATION, data(InvoiceStatus.PAID), NOW);

        invoice.update(data(InvoiceStatus.PENDING), LATER);

        assertThat(invoice.paidAt()).isNull();
        assertThat(invoice.data().status()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    @DisplayName("changing the status to PAID stamps paidAt; today, leaving PAID keeps it")
    void changeStatus() {
        Invoice invoice = Invoice.issue(ORGANIZATION, data(InvoiceStatus.PENDING), NOW);

        invoice.changeStatus(InvoiceStatus.PAID, NOW);
        assertThat(invoice.paidAt()).isEqualTo(NOW);

        invoice.changeStatus(InvoiceStatus.PENDING, LATER);
        assertThat(invoice.paidAt()).isEqualTo(NOW);
        assertThat(invoice.data().status()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    @DisplayName("changing the status keeps the rest of the data")
    void changeStatusKeepsData() {
        Invoice invoice = Invoice.issue(ORGANIZATION, data(InvoiceStatus.PENDING), NOW);

        invoice.changeStatus(InvoiceStatus.CANCELLED, NOW);

        assertThat(invoice.data().description()).isEqualTo("Consultation");
        assertThat(invoice.data().amount()).isEqualByComparingTo("100.00");
    }
}
