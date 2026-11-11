package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.out.DeleteClientInvoicesPort;
import app.lexo.application.port.out.DeleteInvoicePort;
import app.lexo.application.port.out.DetachCaseFromInvoicesPort;
import app.lexo.application.port.out.ListInvoicesPort;
import app.lexo.application.port.out.LoadClientInvoicesPort;
import app.lexo.application.port.out.LoadInvoicePort;
import app.lexo.application.port.out.LoadInvoicesDuePort;
import app.lexo.application.port.out.SaveInvoicePort;
import app.lexo.application.port.out.SumInvoicesPort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.common.web.SortOrder;
import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invoice persistence adapter (integration, Postgres)")
class InvoicePersistenceAdapterIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Autowired
    private SaveInvoicePort save;

    @Autowired
    private LoadInvoicePort load;

    @Autowired
    private DeleteInvoicePort delete;

    @Autowired
    private ListInvoicesPort list;

    @Autowired
    private LoadInvoicesDuePort loadDue;

    @Autowired
    private LoadClientInvoicesPort loadOfClient;

    @Autowired
    private SumInvoicesPort sum;

    @Autowired
    private DeleteClientInvoicesPort deleteOfClient;

    @Autowired
    private DetachCaseFromInvoicesPort detach;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private Invoice saveInvoice(OrganizationId organization, ClientId client, CaseId caseId, String amount,
                                InvoiceStatus status, Instant dueDate) {
        return save.save(Invoice.issue(organization,
                new InvoiceData(client, caseId, "invoice", new BigDecimal(amount), status, dueDate), NOW));
    }

    @Test
    @DisplayName("saving a new invoice generates the id and loads back with all the data")
    void saveNew() {
        OrganizationId organization = newOrganization();
        CaseId caseId = new CaseId("case-1");

        Invoice saved = saveInvoice(organization, new ClientId("client-1"), caseId, "150.00", InvoiceStatus.PAID,
                Instant.parse("2027-01-01T00:00:00Z"));

        assertThat(saved.id()).isNotNull();
        Invoice read = load.byId(organization, saved.id()).orElseThrow();
        assertThat(read.data()).isEqualTo(saved.data());
        assertThat(read.paidAt()).isNotNull();
        assertThat(read.data().status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("the status is stored with the legacy code and read back as the enum")
    void statusCode() {
        OrganizationId organization = newOrganization();
        Invoice saved = saveInvoice(organization, new ClientId("c"), null, "1.00", InvoiceStatus.OVERDUE, NOW);

        String stored = jdbc.queryForObject("select status from invoices where id = ?", String.class, saved.id().value());

        assertThat(stored).isEqualTo("ATRASADO");
    }

    @Test
    @DisplayName("saving an existing invoice updates the same row")
    void saveExisting() {
        OrganizationId organization = newOrganization();
        Invoice saved = saveInvoice(organization, new ClientId("c"), null, "1.00", InvoiceStatus.PENDING, NOW);

        saved.changeStatus(InvoiceStatus.PAID, NOW);
        save.save(saved);

        Invoice read = load.byId(organization, saved.id()).orElseThrow();
        assertThat(read.data().status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(list.list(organization, new PageParams(0, 20, List.of())).totalElements()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("load, delete and save only reach invoices of the invoice's own organization")
    void organizationScope() {
        OrganizationId owner = newOrganization();
        OrganizationId intruder = newOrganization();
        Invoice invoice = saveInvoice(owner, new ClientId("c"), null, "1.00", InvoiceStatus.PENDING, NOW);
        Invoice asIntruder = Invoice.restore(invoice.id(), intruder, invoice.data(), null);

        assertThat(load.byId(intruder, invoice.id())).isEmpty();

        assertThat(delete.delete(intruder, invoice.id())).isFalse();
        assertThat(load.byId(owner, invoice.id())).isPresent();
        assertThatThrownBy(() -> save.save(asIntruder)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("list pages, sorts and shows only the organization")
    void list() {
        OrganizationId organization = newOrganization();
        Instant base = Instant.parse("2027-02-01T00:00:00Z");
        saveInvoice(organization, new ClientId("c"), null, "3.00", InvoiceStatus.PENDING, base.plusSeconds(30));
        saveInvoice(organization, new ClientId("c"), null, "1.00", InvoiceStatus.PENDING, base.plusSeconds(10));
        saveInvoice(organization, new ClientId("c"), null, "2.00", InvoiceStatus.PENDING, base.plusSeconds(20));
        saveInvoice(newOrganization(), new ClientId("c"), null, "9.00", InvoiceStatus.PENDING, base);

        PageResult<Invoice> byDueDate = list.list(organization, new PageParams(0, 20, List.of(new SortOrder("dueDate", true))));
        PageResult<Invoice> secondPage = list.list(organization, new PageParams(1, 2, List.of(new SortOrder("dueDate", true))));

        assertThat(byDueDate.totalElements()).isEqualTo(3);
        assertThat(byDueDate.content()).extracting(i -> i.data().amount().intValue()).containsExactly(1, 2, 3);
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    @DisplayName("sum by status adds only the asked statuses of the organization and gives zero when empty")
    void sumByStatus() {
        OrganizationId organization = newOrganization();
        saveInvoice(organization, new ClientId("c"), null, "100.00", InvoiceStatus.PENDING, NOW);
        saveInvoice(organization, new ClientId("c"), null, "50.50", InvoiceStatus.OVERDUE, NOW);
        saveInvoice(organization, new ClientId("c"), null, "200.00", InvoiceStatus.PAID, NOW);
        saveInvoice(organization, new ClientId("c"), null, "999.00", InvoiceStatus.CANCELLED, NOW);

        assertThat(sum.sumByStatus(organization, Set.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE)))
                .isEqualByComparingTo("150.50");
        assertThat(sum.sumByStatus(organization, Set.of(InvoiceStatus.PAID))).isEqualByComparingTo("200.00");
        assertThat(sum.sumByStatus(newOrganization(), Set.of(InvoiceStatus.PAID))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("due between includes both ends and orders by due date")
    void dueBetween() {
        OrganizationId organization = newOrganization();
        Instant start = Instant.parse("2027-03-10T12:00:00Z");
        Instant end = Instant.parse("2027-03-20T12:00:00Z");
        saveInvoice(organization, new ClientId("c"), null, "1.00", InvoiceStatus.PAID, end);
        saveInvoice(organization, new ClientId("c"), null, "2.00", InvoiceStatus.PAID, start);
        saveInvoice(organization, new ClientId("c"), null, "3.00", InvoiceStatus.PAID, start.minusSeconds(1));
        saveInvoice(organization, new ClientId("c"), null, "4.00", InvoiceStatus.PAID, end.plusSeconds(1));

        List<Invoice> due = loadDue.dueBetween(organization, start, end);

        assertThat(due).extracting(i -> i.data().amount().intValue()).containsExactly(2, 1);
    }

    @Test
    @DisplayName("the client's invoices come ordered by due date and only from that organization")
    void ofClient() {
        OrganizationId organization = newOrganization();
        ClientId client = new ClientId("client-1");
        saveInvoice(organization, client, null, "2.00", InvoiceStatus.PENDING, NOW.plusSeconds(20));
        saveInvoice(organization, client, null, "1.00", InvoiceStatus.PENDING, NOW.plusSeconds(10));
        saveInvoice(organization, new ClientId("other"), null, "9.00", InvoiceStatus.PENDING, NOW);
        saveInvoice(newOrganization(), client, null, "8.00", InvoiceStatus.PENDING, NOW);

        List<Invoice> invoices = loadOfClient.ofClient(organization, client);

        assertThat(invoices).extracting(i -> i.data().amount().intValue()).containsExactly(1, 2);
    }

    @Test
    @Transactional
    @DisplayName("deleting the client's invoices only touches that client in that organization")
    void deleteOfClient() {
        OrganizationId organization = newOrganization();
        OrganizationId other = newOrganization();
        ClientId client = new ClientId("client-1");
        saveInvoice(organization, client, null, "1.00", InvoiceStatus.PENDING, NOW);
        saveInvoice(organization, client, null, "2.00", InvoiceStatus.PAID, NOW);
        saveInvoice(organization, new ClientId("stays"), null, "3.00", InvoiceStatus.PENDING, NOW);
        saveInvoice(other, client, null, "4.00", InvoiceStatus.PENDING, NOW);

        int deleted = deleteOfClient.deleteOfClient(organization, client);

        assertThat(deleted).isEqualTo(2);
        assertThat(loadOfClient.ofClient(organization, client)).isEmpty();
        assertThat(loadOfClient.ofClient(other, client)).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("detaching a case clears it from the invoices without deleting them")
    void detachCase() {
        OrganizationId organization = newOrganization();
        OrganizationId other = newOrganization();
        CaseId caseId = new CaseId("case-1");
        Invoice linked = saveInvoice(organization, new ClientId("c"), caseId, "1.00", InvoiceStatus.PENDING, NOW);
        Invoice otherOrganization = saveInvoice(other, new ClientId("c"), caseId, "2.00", InvoiceStatus.PENDING, NOW);

        int detached = detach.detach(organization, caseId);

        assertThat(detached).isEqualTo(1);
        assertThat(jdbc.queryForObject("select case_id from invoices where id = ?", String.class,
                linked.id().value())).isNull();
        assertThat(jdbc.queryForObject("select case_id from invoices where id = ?", String.class,
                otherOrganization.id().value())).isEqualTo("case-1");
    }
}
