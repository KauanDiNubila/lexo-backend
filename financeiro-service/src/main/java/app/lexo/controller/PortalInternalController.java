package app.lexo.controller;

import app.lexo.dto.PortalDtos.HonorarioPortal;
import app.lexo.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint interno (servico-a-servico) que devolve os honorarios de um cliente.
 * Usado pelo cliente-service para montar o portal do cliente.
 */
@RestController
public class PortalInternalController {

    private final InvoiceRepository repo;
    private final String chaveInterna;

    public PortalInternalController(InvoiceRepository repo,
                                    @Value("${lexo.internal.api-key}") String chaveInterna) {
        this.repo = repo;
        this.chaveInterna = chaveInterna;
    }

    @GetMapping("/internal/portal/honorarios")
    public List<HonorarioPortal> porCliente(@RequestParam String clientId, @RequestParam String orgId,
                                            @RequestHeader(value = "X-Internal-Key", required = false) String chave) {
        if (!chaveInterna.equals(chave)) {
            throw ApiException.forbidden("Acesso interno negado");
        }
        return repo.findByClientIdAndOrganizationIdOrderByDueDateAsc(clientId, orgId).stream()
                .map(i -> new HonorarioPortal(i.getDescription(), i.getAmount(), i.getStatus().name(), i.getDueDate()))
                .toList();
    }
}
