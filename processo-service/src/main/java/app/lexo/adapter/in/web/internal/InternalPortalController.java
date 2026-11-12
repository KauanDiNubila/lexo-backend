package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.CaseOverview;
import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.ListClientCasesQuery;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.common.portal.PortalDtos.PortalDeadline;
import app.lexo.common.portal.PortalDtos.PortalUpdate;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class InternalPortalController {

    private final ListClientCasesQuery listClientCases;

    InternalPortalController(ListClientCasesQuery listClientCases) {
        this.listClientCases = listClientCases;
    }

    @GetMapping("/internal/portal/processos")
    List<PortalCase> byClient(@RequestParam String clientId, @RequestParam String orgId) {
        if (clientId.isBlank() || orgId.isBlank()) {
            return List.of();
        }
        return listClientCases.list(new OrganizationId(orgId), new ClientId(clientId)).stream()
                .map(InternalPortalController::toPortal)
                .toList();
    }

    private static PortalCase toPortal(CaseOverview overview) {
        CaseResponse value = overview.caseData();
        return new PortalCase(
                value.number(),
                value.area(),
                value.status().code(),
                value.createdAt(),
                overview.deadlines().stream()
                        .map(deadline -> new PortalDeadline(deadline.title(), deadline.type().code(),
                                deadline.status().code(), deadline.date()))
                        .toList(),
                overview.movements().stream()
                        .map(movement -> new PortalUpdate(movement.title(), movement.description(), movement.date()))
                        .toList());
    }
}
