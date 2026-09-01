package app.lexo.controller;

import app.lexo.common.portal.PortalDtos.AndamentoPortal;
import app.lexo.common.portal.PortalDtos.PrazoPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.Andamento;
import app.lexo.domain.Case;
import app.lexo.domain.Deadline;
import app.lexo.repository.AndamentoRepository;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalInternalController {

    private final CaseRepository casos;
    private final DeadlineRepository prazos;
    private final AndamentoRepository andamentos;

    public PortalInternalController(CaseRepository casos, DeadlineRepository prazos,
                                    AndamentoRepository andamentos) {
        this.casos = casos;
        this.prazos = prazos;
        this.andamentos = andamentos;
    }

    @GetMapping("/internal/portal/processos")
    public List<ProcessoPortal> porCliente(@RequestParam String clientId, @RequestParam String orgId) {
        List<Case> lista = casos.findByClientIdAndOrganizationIdOrderByCreatedAtDesc(clientId, orgId);
        if (lista.isEmpty()) {
            return List.of();
        }
        List<String> ids = lista.stream().map(Case::getId).toList();

        Map<String, List<PrazoPortal>> prazosPorCaso = prazos
                .findByCaseIdInAndOrganizationIdOrderByDateAsc(ids, orgId).stream()
                .collect(Collectors.groupingBy(Deadline::getCaseId,
                        Collectors.mapping(this::paraPrazo, Collectors.toList())));

        Map<String, List<AndamentoPortal>> andamentosPorCaso = andamentos
                .findByCaseIdInAndOrganizationIdOrderByDateDesc(ids, orgId).stream()
                .collect(Collectors.groupingBy(Andamento::getCaseId,
                        Collectors.mapping(this::paraAndamento, Collectors.toList())));

        return lista.stream()
                .map(c -> new ProcessoPortal(
                        c.getNumber(),
                        c.getArea(),
                        c.getStatus().name(),
                        c.getCreatedAt(),
                        prazosPorCaso.getOrDefault(c.getId(), List.of()),
                        andamentosPorCaso.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private PrazoPortal paraPrazo(Deadline d) {
        return new PrazoPortal(d.getTitle(), d.getType().name(), d.getStatus().name(), d.getDate());
    }

    private AndamentoPortal paraAndamento(Andamento a) {
        return new AndamentoPortal(a.getTitle(), a.getDescription(), a.getDate());
    }
}
