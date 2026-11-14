package app.lexo.adapter.out.persistence;

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
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
class InvoicePersistenceAdapter implements SaveInvoicePort, LoadInvoicePort, DeleteInvoicePort, ListInvoicesPort,
        LoadInvoicesDuePort, LoadClientInvoicesPort, SumInvoicesPort, DeleteClientInvoicesPort,
        DetachCaseFromInvoicesPort {

    private final SpringDataInvoiceRepository repository;

    InvoicePersistenceAdapter(SpringDataInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = invoice.id() == null
                ? new InvoiceJpaEntity()
                : repository.findByIdAndOrganizationId(invoice.id().value(), invoice.organizationId().value())
                        .orElseThrow(() -> new IllegalStateException(
                                "Invoice " + invoice.id().value() + " does not exist to update"));
        InvoiceMapper.copyTo(invoice, entity);
        return InvoiceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Invoice> byId(OrganizationId organizationId, InvoiceId invoiceId) {
        return repository.findByIdAndOrganizationId(invoiceId.value(), organizationId.value())
                .map(InvoiceMapper::toDomain);
    }

    @Override
    public boolean delete(OrganizationId organizationId, InvoiceId invoiceId) {
        return repository.deleteByIdAndOrganizationId(invoiceId.value(), organizationId.value()) > 0;
    }

    @Override
    public PageResult<Invoice> list(OrganizationId organizationId, PageParams pageParams) {
        return PageResult.of(repository.findByOrganizationId(organizationId.value(),
                PageParamsMapper.toPageRequest(pageParams)), InvoiceMapper::toDomain);
    }

    @Override
    public List<Invoice> dueBetween(OrganizationId organizationId, Instant start, Instant end) {
        return repository.findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(organizationId.value(), start, end)
                .stream().map(InvoiceMapper::toDomain).toList();
    }

    @Override
    public List<Invoice> ofClient(OrganizationId organizationId, ClientId clientId) {
        return repository.findByClientIdAndOrganizationIdOrderByDueDateAsc(clientId.value(), organizationId.value())
                .stream().map(InvoiceMapper::toDomain).toList();
    }

    @Override
    public BigDecimal sumByStatus(OrganizationId organizationId, Set<InvoiceStatus> statuses) {
        return repository.sumByStatus(organizationId.value(), statuses);
    }

    @Override
    public int deleteOfClient(OrganizationId organizationId, ClientId clientId) {
        return (int) repository.deleteByClientIdAndOrganizationId(clientId.value(), organizationId.value());
    }

    @Override
    public int detach(OrganizationId organizationId, CaseId caseId) {
        return repository.detachCase(caseId.value(), organizationId.value());
    }
}
