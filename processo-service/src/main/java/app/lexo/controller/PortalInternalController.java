package app.lexo.controller;

import app.lexo.domain.Case;
import app.lexo.domain.Deadline;
import app.lexo.dto.PortalDtos.PrazoPortal;
import app.lexo.dto.PortalDtos.ProcessoPortal;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint interno (servico-a-servico) que devolve os processos de um cliente, cada um com
 * seus prazos. Usado pelo cliente-service para montar o portal do cliente.
 */
@RestController
public class PortalInternalController {

    private final CaseRepository casos;
    private final DeadlineRepository prazos;
    private final String chaveInterna;

    public PortalInternalController(CaseRepository casos, DeadlineRepository prazos,
                                    @Value("${lexo.internal.api-key}") String chaveInterna) {
        this.casos = casos;
        this.prazos = prazos;
        this.chaveInterna = chaveInterna;
    }

    @GetMapping("/internal/portal/processos")
    public List<ProcessoPortal> porCliente(@RequestParam String clientId, @RequestParam String orgId,
                                           @RequestHeader(value = "X-Internal-Key", required = false) String chave) {
        if (!chaveInterna.equals(chave)) {
            throw ApiException.forbidden("Acesso interno negado");
        }
        List<Case> lista = casos.findByClientIdAndOrganizationIdOrderByCreatedAtDesc(clientId, orgId);
        if (lista.isEmpty()) {
            return List.of();
        }
        List<String> ids = lista.stream().map(Case::getId).toList();
        Map<String, List<PrazoPortal>> porCaso = prazos
                .findByCaseIdInAndOrganizationIdOrderByDateAsc(ids, orgId).stream()
                .collect(Collectors.groupingBy(Deadline::getCaseId,
                        Collectors.mapping(this::paraPrazo, Collectors.toList())));

        return lista.stream()
                .map(c -> new ProcessoPortal(
                        c.getNumber(),
                        c.getArea(),
                        c.getStatus().name(),
                        c.getCreatedAt(),
                        porCaso.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private PrazoPortal paraPrazo(Deadline d) {
        return new PrazoPortal(d.getTitle(), d.getType().name(), d.getStatus().name(), d.getDate());
    }
}
