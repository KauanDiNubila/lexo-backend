package app.lexo.controller;

import app.lexo.common.portal.PortalDtos.PortalUpdate;
import app.lexo.common.portal.PortalDtos.PortalDeadline;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.domain.LegacyAndamento;
import app.lexo.domain.LegacyCase;
import app.lexo.domain.LegacyDeadline;
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
    public List<PortalCase> porCliente(@RequestParam String clientId, @RequestParam String orgId) {
        List<LegacyCase> lista = casos.findByClientIdAndOrganizationIdOrderByCreatedAtDesc(clientId, orgId);
        if (lista.isEmpty()) {
            return List.of();
        }
        List<String> ids = lista.stream().map(LegacyCase::getId).toList();

        Map<String, List<PortalDeadline>> prazosPorCaso = prazos
                .findByCaseIdInAndOrganizationIdOrderByDateAsc(ids, orgId).stream()
                .collect(Collectors.groupingBy(LegacyDeadline::getCaseId,
                        Collectors.mapping(this::paraPrazo, Collectors.toList())));

        Map<String, List<PortalUpdate>> andamentosPorCaso = andamentos
                .findByCaseIdInAndOrganizationIdOrderByDateDesc(ids, orgId).stream()
                .collect(Collectors.groupingBy(LegacyAndamento::getCaseId,
                        Collectors.mapping(this::paraAndamento, Collectors.toList())));

        return lista.stream()
                .map(c -> new PortalCase(
                        c.getNumber(),
                        c.getArea(),
                        c.getStatus().name(),
                        c.getCreatedAt(),
                        prazosPorCaso.getOrDefault(c.getId(), List.of()),
                        andamentosPorCaso.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private PortalDeadline paraPrazo(LegacyDeadline d) {
        return new PortalDeadline(d.getTitle(), d.getType().name(), d.getStatus().name(), d.getDate());
    }

    private PortalUpdate paraAndamento(LegacyAndamento a) {
        return new PortalUpdate(a.getTitle(), a.getDescription(), a.getDate());
    }
}
