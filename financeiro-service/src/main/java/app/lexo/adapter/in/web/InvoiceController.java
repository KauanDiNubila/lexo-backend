package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ChangeInvoiceStatusCommand;
import app.lexo.application.port.in.ChangeInvoiceStatusUseCase;
import app.lexo.application.port.in.CreateInvoiceCommand;
import app.lexo.application.port.in.CreateInvoiceUseCase;
import app.lexo.application.port.in.DeleteInvoiceCommand;
import app.lexo.application.port.in.DeleteInvoiceUseCase;
import app.lexo.application.port.in.FinancialReport;
import app.lexo.application.port.in.FinancialSummary;
import app.lexo.application.port.in.GetFinancialReportQuery;
import app.lexo.application.port.in.GetFinancialSummaryQuery;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.in.ListInvoicesQuery;
import app.lexo.application.port.in.UpdateInvoiceCommand;
import app.lexo.application.port.in.UpdateInvoiceUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.InvoiceId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasRole('ADVOGADO')")
class InvoiceController {

    private final CreateInvoiceUseCase createInvoice;
    private final UpdateInvoiceUseCase updateInvoice;
    private final ChangeInvoiceStatusUseCase changeInvoiceStatus;
    private final DeleteInvoiceUseCase deleteInvoice;
    private final ListInvoicesQuery listInvoices;
    private final GetFinancialSummaryQuery financialSummary;
    private final GetFinancialReportQuery financialReport;

    InvoiceController(CreateInvoiceUseCase createInvoice, UpdateInvoiceUseCase updateInvoice,
                      ChangeInvoiceStatusUseCase changeInvoiceStatus, DeleteInvoiceUseCase deleteInvoice,
                      ListInvoicesQuery listInvoices, GetFinancialSummaryQuery financialSummary,
                      GetFinancialReportQuery financialReport) {
        this.createInvoice = createInvoice;
        this.updateInvoice = updateInvoice;
        this.changeInvoiceStatus = changeInvoiceStatus;
        this.deleteInvoice = deleteInvoice;
        this.listInvoices = listInvoices;
        this.financialSummary = financialSummary;
        this.financialReport = financialReport;
    }

    @GetMapping
    PageResult<InvoiceResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return listInvoices.list(Identity.organization(me), PageParamsMapper.from(pageable));
    }

    @GetMapping("/resumo")
    FinancialSummary summary(@AuthenticationPrincipal AuthUser me) {
        return financialSummary.summarize(Identity.organization(me));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody InvoiceRequest req) {
        return createInvoice.create(new CreateInvoiceCommand(Identity.organization(me), req.toData()));
    }

    @PutMapping("/{id}")
    InvoiceResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                           @Valid @RequestBody InvoiceRequest req) {
        return updateInvoice.update(new UpdateInvoiceCommand(Identity.organization(me), new InvoiceId(id), req.toData()));
    }

    @PatchMapping("/{id}/status")
    InvoiceResponse changeStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                 @Valid @RequestBody StatusRequest req) {
        return changeInvoiceStatus.changeStatus(
                new ChangeInvoiceStatusCommand(Identity.organization(me), new InvoiceId(id), req.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        deleteInvoice.delete(new DeleteInvoiceCommand(Identity.organization(me), new InvoiceId(id)));
    }

    @GetMapping("/relatorio")
    FinancialReport report(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return financialReport.report(Identity.organization(me), start, end);
    }
}
