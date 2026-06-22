package app.lexo.controller;

import app.lexo.dto.InvoiceDtos.FinancialReport;
import app.lexo.dto.InvoiceDtos.InvoiceRequest;
import app.lexo.dto.InvoiceDtos.InvoiceResponse;
import app.lexo.dto.InvoiceDtos.StatusRequest;
import app.lexo.security.AuthUser;
import app.lexo.service.InvoiceService;
import jakarta.validation.Valid;
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
import java.util.List;

/**
 * Financeiro/honorarios. Vedado a SECRETARIA — exige ADMIN ou ADVOGADO em toda rota
 * (autorizacao no ponto de uso, nao apenas no roteamento).
 */
@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasAnyRole('ADMIN','ADVOGADO')")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<InvoiceResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@AuthenticationPrincipal AuthUser me,
                                  @Valid @RequestBody InvoiceRequest req) {
        return service.create(me, req);
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                  @Valid @RequestBody InvoiceRequest req) {
        return service.update(me, id, req);
    }

    @PatchMapping("/{id}/status")
    public InvoiceResponse updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                        @Valid @RequestBody StatusRequest req) {
        return service.updateStatus(me, id, req.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.delete(me, id);
    }

    @GetMapping("/relatorio")
    public FinancialReport report(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return service.report(me, start, end);
    }
}
