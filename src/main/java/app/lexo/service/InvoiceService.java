package app.lexo.service;

import app.lexo.domain.Invoice;
import app.lexo.domain.enums.InvoiceStatus;
import app.lexo.dto.InvoiceDtos.FinancialReport;
import app.lexo.dto.InvoiceDtos.InvoiceRequest;
import app.lexo.dto.InvoiceDtos.InvoiceResponse;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.ClientRepository;
import app.lexo.repository.InvoiceRepository;
import app.lexo.security.AuthUser;
import app.lexo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Honorarios. Toda mutacao/leitura exige ADMIN/ADVOGADO (SECRETARIA e barrada) — a
 * verificacao acontece no ponto de uso, no controller, via @PreAuthorize.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository repo;
    private final ClientRepository clientRepo;
    private final CaseRepository caseRepo;

    public InvoiceService(InvoiceRepository repo, ClientRepository clientRepo, CaseRepository caseRepo) {
        this.repo = repo;
        this.clientRepo = clientRepo;
        this.caseRepo = caseRepo;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list(AuthUser me) {
        return repo.findByOrganizationIdOrderByDueDateAsc(me.organizationId())
                .stream().map(InvoiceResponse::from).toList();
    }

    @Transactional
    public InvoiceResponse create(AuthUser me, InvoiceRequest req) {
        validateRefs(me, req);
        Invoice i = new Invoice();
        i.setOrganizationId(me.organizationId());
        apply(i, req);
        if (req.status() == InvoiceStatus.PAGO) {
            i.setPaidAt(Instant.now());
        }
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public InvoiceResponse update(AuthUser me, String id, InvoiceRequest req) {
        validateRefs(me, req);
        Invoice i = load(me, id);
        apply(i, req);
        i.setPaidAt(req.status() == InvoiceStatus.PAGO ? Instant.now() : null);
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public InvoiceResponse updateStatus(AuthUser me, String id, InvoiceStatus status) {
        Invoice i = load(me, id);
        i.setStatus(status);
        if (status == InvoiceStatus.PAGO) {
            i.setPaidAt(Instant.now());
        }
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public void delete(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    @Transactional(readOnly = true)
    public FinancialReport report(AuthUser me, Instant start, Instant end) {
        List<Invoice> invoices = repo
                .findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(me.organizationId(), start, end);

        BigDecimal pago = sumByStatus(invoices, InvoiceStatus.PAGO);
        BigDecimal pendente = sumByStatus(invoices, InvoiceStatus.PENDENTE);
        BigDecimal atrasado = sumByStatus(invoices, InvoiceStatus.ATRASADO);

        return new FinancialReport(start, end, pago, pendente, atrasado,
                invoices.stream().map(InvoiceResponse::from).toList());
    }

    private BigDecimal sumByStatus(List<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream()
                .filter(i -> i.getStatus() == status)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice load(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Honorário não encontrado"));
    }

    private void validateRefs(AuthUser me, InvoiceRequest req) {
        if (!clientRepo.existsByIdAndOrganizationId(req.clientId(), me.organizationId())) {
            throw ApiException.notFound("Cliente não encontrado");
        }
        if (req.caseId() != null && !req.caseId().isBlank()
                && !caseRepo.existsByIdAndOrganizationId(req.caseId(), me.organizationId())) {
            throw ApiException.notFound("Processo não encontrado");
        }
    }

    private void apply(Invoice i, InvoiceRequest req) {
        i.setClientId(req.clientId());
        i.setCaseId((req.caseId() == null || req.caseId().isBlank()) ? null : req.caseId());
        i.setDescription(req.description());
        i.setAmount(req.amount());
        i.setStatus(req.status());
        i.setDueDate(req.dueDate());
    }
}
