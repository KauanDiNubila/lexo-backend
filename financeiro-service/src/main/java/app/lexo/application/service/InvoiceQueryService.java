package app.lexo.application.service;

import app.lexo.application.port.in.FinancialReport;
import app.lexo.application.port.in.FinancialSummary;
import app.lexo.application.port.in.GetFinancialReportQuery;
import app.lexo.application.port.in.GetFinancialSummaryQuery;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.in.ListClientFeesQuery;
import app.lexo.application.port.in.ListInvoicesQuery;
import app.lexo.application.port.out.ListInvoicesPort;
import app.lexo.application.port.out.LoadClientInvoicesPort;
import app.lexo.application.port.out.LoadInvoicesDuePort;
import app.lexo.application.port.out.SumInvoicesPort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.InvoiceTotals;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
class InvoiceQueryService implements ListInvoicesQuery, GetFinancialSummaryQuery, GetFinancialReportQuery,
        ListClientFeesQuery {

    private static final Set<InvoiceStatus> OUTSTANDING = Set.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
    private static final Set<InvoiceStatus> RECEIVED = Set.of(InvoiceStatus.PAID);

    private final ListInvoicesPort listInvoicesPort;
    private final SumInvoicesPort sumInvoicesPort;
    private final LoadInvoicesDuePort loadInvoicesDuePort;
    private final LoadClientInvoicesPort loadClientInvoicesPort;

    InvoiceQueryService(ListInvoicesPort listInvoicesPort, SumInvoicesPort sumInvoicesPort,
                        LoadInvoicesDuePort loadInvoicesDuePort, LoadClientInvoicesPort loadClientInvoicesPort) {
        this.listInvoicesPort = listInvoicesPort;
        this.sumInvoicesPort = sumInvoicesPort;
        this.loadInvoicesDuePort = loadInvoicesDuePort;
        this.loadClientInvoicesPort = loadClientInvoicesPort;
    }

    @Override
    public PageResult<InvoiceResponse> list(OrganizationId organizationId, PageParams pageParams) {
        PageResult<Invoice> page = listInvoicesPort.list(organizationId, pageParams);
        List<InvoiceResponse> content = page.content().stream().map(InvoiceResponse::of).toList();
        return new PageResult<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    public FinancialSummary summarize(OrganizationId organizationId) {
        return new FinancialSummary(
                sumInvoicesPort.sumByStatus(organizationId, OUTSTANDING),
                sumInvoicesPort.sumByStatus(organizationId, RECEIVED));
    }

    @Override
    public FinancialReport report(OrganizationId organizationId, Instant start, Instant end) {
        InvoiceTotals totals = InvoiceTotals.of(loadInvoicesDuePort.dueBetween(organizationId, start, end));
        return new FinancialReport(start, end, totals.paid(), totals.pending(), totals.overdue());
    }

    @Override
    public List<InvoiceResponse> list(OrganizationId organizationId, ClientId clientId) {
        return loadClientInvoicesPort.ofClient(organizationId, clientId).stream().map(InvoiceResponse::of).toList();
    }
}
