package app.lexo.service;

import app.lexo.client.ReferenciaGateway;
import app.lexo.domain.Invoice;
import app.lexo.domain.enums.InvoiceStatus;
import app.lexo.dto.InvoiceDtos.FinancialReport;
import app.lexo.dto.InvoiceDtos.InvoiceRequest;
import app.lexo.dto.InvoiceDtos.InvoiceResponse;
import app.lexo.repository.InvoiceRepository;
import app.lexo.security.AuthUser;
import app.lexo.controller.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class FinanceiroService {

    private final InvoiceRepository repo;
    private final ReferenciaGateway referencias;

    public FinanceiroService(InvoiceRepository repo, ReferenciaGateway referencias) {
        this.repo = repo;
        this.referencias = referencias;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listar(AuthUser me) {
        return repo.findByOrganizationIdOrderByDueDateAsc(me.organizationId())
                .stream().map(InvoiceResponse::from).toList();
    }

    @Transactional
    public InvoiceResponse criar(AuthUser me, InvoiceRequest req) {
        validarReferencias(me, req);
        Invoice i = new Invoice();
        i.setOrganizationId(me.organizationId());
        preencher(i, req);
        if (req.status() == InvoiceStatus.PAGO) {
            i.setPaidAt(Instant.now());
        }
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public InvoiceResponse atualizar(AuthUser me, String id, InvoiceRequest req) {
        validarReferencias(me, req);
        Invoice i = carregar(me, id);
        preencher(i, req);
        i.setPaidAt(req.status() == InvoiceStatus.PAGO ? Instant.now() : null);
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public InvoiceResponse atualizarStatus(AuthUser me, String id, InvoiceStatus status) {
        Invoice i = carregar(me, id);
        i.setStatus(status);
        if (status == InvoiceStatus.PAGO) {
            i.setPaidAt(Instant.now());
        }
        return InvoiceResponse.from(repo.save(i));
    }

    @Transactional
    public void excluir(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    @Transactional(readOnly = true)
    public FinancialReport relatorio(AuthUser me, Instant start, Instant end) {
        List<Invoice> invoices = repo
                .findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(me.organizationId(), start, end);

        BigDecimal pago = somarPorStatus(invoices, InvoiceStatus.PAGO);
        BigDecimal pendente = somarPorStatus(invoices, InvoiceStatus.PENDENTE);
        BigDecimal atrasado = somarPorStatus(invoices, InvoiceStatus.ATRASADO);

        return new FinancialReport(start, end, pago, pendente, atrasado,
                invoices.stream().map(InvoiceResponse::from).toList());
    }

    private BigDecimal somarPorStatus(List<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream()
                .filter(i -> i.getStatus() == status)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice carregar(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Honorário não encontrado"));
    }

    private void validarReferencias(AuthUser me, InvoiceRequest req) {

        if (!referencias.clienteExiste(req.clientId(), me.organizationId())) {
            throw ApiException.notFound("Cliente não encontrado");
        }

        if (req.caseId() != null && !req.caseId().isBlank()
                && !referencias.processoExiste(req.caseId(), me.organizationId())) {
            throw ApiException.notFound("Processo não encontrado");
        }
    }

    private void preencher(Invoice i, InvoiceRequest req) {
        i.setClientId(req.clientId());
        i.setCaseId((req.caseId() == null || req.caseId().isBlank()) ? null : req.caseId());
        i.setDescription(req.description());
        i.setAmount(req.amount());
        i.setStatus(req.status());
        i.setDueDate(req.dueDate());
    }
}
