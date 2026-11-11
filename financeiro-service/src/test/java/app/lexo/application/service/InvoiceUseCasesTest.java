package app.lexo.application.service;

import app.lexo.application.port.in.ChangeInvoiceStatusCommand;
import app.lexo.application.port.in.CreateInvoiceCommand;
import app.lexo.application.port.in.DeleteInvoiceCommand;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.in.UpdateInvoiceCommand;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.ClientId;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.InvoiceNotFoundException;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invoice use cases (in-memory ports)")
class InvoiceUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("organization-1");
    private static final OrganizationId OTHER_ORGANIZATION = new OrganizationId("organization-2");
    private static final ClientId CLIENT = new ClientId("client-1");
    private static final CaseId CASE = new CaseId("case-1");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryInvoices invoices = new InMemoryInvoices();
    private final Set<String> knownClients = new HashSet<>(Set.of("client-1"));
    private final Set<String> knownCases = new HashSet<>(Set.of("case-1"));
    private final InvoiceReferences references = new InvoiceReferences(
            (organization, client) -> knownClients.contains(client.value()),
            (organization, caseId) -> knownCases.contains(caseId.value()));

    private final CreateInvoiceService create = new CreateInvoiceService(references, invoices, clock);
    private final UpdateInvoiceService update = new UpdateInvoiceService(references, invoices, invoices, clock);
    private final ChangeInvoiceStatusService changeStatus = new ChangeInvoiceStatusService(invoices, invoices, clock);
    private final DeleteInvoiceService delete = new DeleteInvoiceService(invoices, invoices);

    private InvoiceData data(ClientId client, CaseId caseId, InvoiceStatus status) {
        return new InvoiceData(client, caseId, "Consultation", new BigDecimal("100.00"), status,
                Instant.parse("2027-01-01T00:00:00Z"));
    }

    private InvoiceResponse issue(InvoiceStatus status) {
        return create.create(new CreateInvoiceCommand(ORGANIZATION, data(CLIENT, null, status)));
    }

    @Test
    @DisplayName("create saves the invoice and stamps paidAt only when PAID")
    void create() {
        InvoiceResponse pending = issue(InvoiceStatus.PENDING);
        InvoiceResponse paid = issue(InvoiceStatus.PAID);

        assertThat(pending.id()).isNotBlank();
        assertThat(pending.paidAt()).isNull();
        assertThat(paid.paidAt()).isEqualTo(NOW);
        assertThat(invoices.all()).hasSize(2);
    }

    @Test
    @DisplayName("create with an unknown client or case fails with not found and saves nothing")
    void createWithUnknownReferences() {
        assertThatThrownBy(() -> create.create(new CreateInvoiceCommand(ORGANIZATION,
                data(new ClientId("unknown"), null, InvoiceStatus.PENDING)))).isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> create.create(new CreateInvoiceCommand(ORGANIZATION,
                data(CLIENT, new CaseId("unknown"), InvoiceStatus.PENDING)))).isInstanceOf(CaseNotFoundException.class);

        assertThat(invoices.all()).isEmpty();
    }

    @Test
    @DisplayName("create without a case does not ask the case port")
    void createWithoutCase() {
        knownCases.clear();

        InvoiceResponse response = issue(InvoiceStatus.PENDING);

        assertThat(response.caseId()).isNull();
    }

    @Test
    @DisplayName("create with a known case keeps it on the invoice")
    void createWithCase() {
        InvoiceResponse response = create.create(new CreateInvoiceCommand(ORGANIZATION,
                data(CLIENT, CASE, InvoiceStatus.PENDING)));

        assertThat(response.caseId()).isEqualTo("case-1");
    }

    @Test
    @DisplayName("the command rejects a blank description, a non positive amount and a missing due date")
    void commandValidation() {
        InvoiceData blank = new InvoiceData(CLIENT, null, " ", new BigDecimal("1.00"), InvoiceStatus.PENDING, NOW);
        InvoiceData zero = new InvoiceData(CLIENT, null, "x", BigDecimal.ZERO, InvoiceStatus.PENDING, NOW);
        InvoiceData noDueDate = new InvoiceData(CLIENT, null, "x", new BigDecimal("1.00"), InvoiceStatus.PENDING, null);

        assertThatThrownBy(() -> new CreateInvoiceCommand(ORGANIZATION, blank)).isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new CreateInvoiceCommand(ORGANIZATION, zero)).isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new UpdateInvoiceCommand(ORGANIZATION, new InvoiceId("i1"), noDueDate))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    @DisplayName("update replaces the data; from PAID to another status it clears paidAt")
    void update() {
        InvoiceResponse paid = issue(InvoiceStatus.PAID);

        InvoiceResponse updated = update.update(new UpdateInvoiceCommand(ORGANIZATION, new InvoiceId(paid.id()),
                data(CLIENT, null, InvoiceStatus.PENDING)));

        assertThat(updated.id()).isEqualTo(paid.id());
        assertThat(updated.status()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(updated.paidAt()).isNull();
    }

    @Test
    @DisplayName("update checks the references before looking for the invoice")
    void updateChecksReferencesFirst() {
        assertThatThrownBy(() -> update.update(new UpdateInvoiceCommand(ORGANIZATION, new InvoiceId("missing"),
                data(new ClientId("unknown"), null, InvoiceStatus.PENDING)))).isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("update of an invoice of another organization returns not found")
    void updateFromOtherOrganization() {
        InvoiceResponse created = issue(InvoiceStatus.PENDING);

        assertThatThrownBy(() -> update.update(new UpdateInvoiceCommand(OTHER_ORGANIZATION,
                new InvoiceId(created.id()), data(CLIENT, null, InvoiceStatus.PENDING))))
                .isInstanceOf(InvoiceNotFoundException.class)
                .hasMessage("Honor\u00e1rio n\u00e3o encontrado");
    }

    @Test
    @DisplayName("change status to PAID stamps paidAt and does not ask the client or case port")
    void changeStatus() {
        InvoiceResponse created = issue(InvoiceStatus.PENDING);
        knownClients.clear();

        InvoiceResponse changed = changeStatus.changeStatus(new ChangeInvoiceStatusCommand(ORGANIZATION,
                new InvoiceId(created.id()), InvoiceStatus.PAID));

        assertThat(changed.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(changed.paidAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("change status of an invoice of another organization returns not found")
    void changeStatusFromOtherOrganization() {
        InvoiceResponse created = issue(InvoiceStatus.PENDING);

        assertThatThrownBy(() -> changeStatus.changeStatus(new ChangeInvoiceStatusCommand(OTHER_ORGANIZATION,
                new InvoiceId(created.id()), InvoiceStatus.PAID))).isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes the invoice; another organization and a second delete get not found")
    void delete() {
        InvoiceResponse created = issue(InvoiceStatus.PENDING);
        InvoiceId id = new InvoiceId(created.id());

        assertThatThrownBy(() -> delete.delete(new DeleteInvoiceCommand(OTHER_ORGANIZATION, id)))
                .isInstanceOf(InvoiceNotFoundException.class);
        delete.delete(new DeleteInvoiceCommand(ORGANIZATION, id));

        assertThat(invoices.all()).isEmpty();
        assertThatThrownBy(() -> delete.delete(new DeleteInvoiceCommand(ORGANIZATION, id)))
                .isInstanceOf(InvoiceNotFoundException.class);
    }
}
