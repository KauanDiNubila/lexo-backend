package app.lexo.controller;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.repository.InvoiceRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalInternalController {

    private final InvoiceRepository repo;

    public PortalInternalController(InvoiceRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/internal/portal/honorarios")
    public List<HonorarioPortal> porCliente(@RequestParam String clientId, @RequestParam String orgId) {
        return repo.findByClientIdAndOrganizationIdOrderByDueDateAsc(clientId, orgId).stream()
                .map(i -> new HonorarioPortal(i.getDescription(), i.getAmount(), i.getStatus().name(), i.getDueDate()))
                .toList();
    }
}
