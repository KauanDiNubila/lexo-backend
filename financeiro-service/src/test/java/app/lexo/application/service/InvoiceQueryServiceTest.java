package app.lexo.application.service;

import app.lexo.application.port.in.FinancialReport;
import app.lexo.application.port.in.FinancialSummary;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.Pagina;
import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvoiceQueryService")
class InvoiceQueryServiceTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("organization-1");
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    private Invoice invoice(String amount, InvoiceStatus status, String description) {
        return Invoice.restore(new app.lexo.domain.InvoiceId("i-" + description), ORGANIZATION,
                new InvoiceData(new ClientId("client-1"), null, description, new BigDecimal(amount), status,
                        Instant.parse("2027-01-01T00:00:00Z")), null);
    }

    @Test
    @DisplayName("list converts the domain page into responses and keeps the totals")
    void list() {
        Invoice invoice = invoice("10.00", InvoiceStatus.PENDING, "a");
        InvoiceQueryService service = new InvoiceQueryService(
                (organization, params) -> new Pagina<>(List.of(invoice), 0, 20, 1, 1), null, null, null);

        Pagina<InvoiceResponse> page = service.list(ORGANIZATION, new PageParams(0, 20, List.of()));

        assertThat(page.content()).extracting(InvoiceResponse::description).containsExactly("a");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("summary asks for PENDING plus OVERDUE as outstanding and PAID as received")
    void summary() {
        Map<Set<InvoiceStatus>, BigDecimal> sums = Map.of(
                Set.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE), new BigDecimal("150.50"),
                Set.of(InvoiceStatus.PAID), new BigDecimal("200.00"));
        InvoiceQueryService service = new InvoiceQueryService(null,
                (organization, statuses) -> sums.get(statuses), null, null);

        FinancialSummary summary = service.summarize(ORGANIZATION);

        assertThat(summary.outstanding()).isEqualByComparingTo("150.50");
        assertThat(summary.received()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("report sums by status over the invoices due in the period")
    void report() {
        InvoiceQueryService service = new InvoiceQueryService(null, null,
                (organization, start, end) -> List.of(
                        invoice("100.00", InvoiceStatus.PAID, "a"), invoice("40.50", InvoiceStatus.PENDING, "b"),
                        invoice("10.25", InvoiceStatus.OVERDUE, "c"), invoice("999.00", InvoiceStatus.CANCELLED, "d")),
                null);

        FinancialReport report = service.report(ORGANIZATION, NOW, NOW.plusSeconds(60));

        assertThat(report.totalPaid()).isEqualByComparingTo("100.00");
        assertThat(report.totalPending()).isEqualByComparingTo("40.50");
        assertThat(report.totalOverdue()).isEqualByComparingTo("10.25");
        assertThat(report.start()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("client fees list the client's invoices with their domain status")
    void clientFees() {
        InvoiceQueryService service = new InvoiceQueryService(null, null, null,
                (organization, client) -> List.of(invoice("10.00", InvoiceStatus.OVERDUE, "late")));

        List<InvoiceResponse> fees = service.list(ORGANIZATION, new ClientId("client-1"));

        assertThat(fees).singleElement().satisfies(fee -> {
            assertThat(fee.description()).isEqualTo("late");
            assertThat(fee.status()).isEqualTo(InvoiceStatus.OVERDUE);
            assertThat(fee.amount()).isEqualByComparingTo("10.00");
        });
    }
}
