package app.lexo.application.service;

import app.lexo.application.port.in.ListMovementsQuery;
import app.lexo.application.port.in.MovementResponse;
import app.lexo.application.port.out.ListMovementsPort;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class MovementQueryService implements ListMovementsQuery {

    private final CaseGuard guard;
    private final ListMovementsPort listMovementsPort;

    MovementQueryService(CaseGuard guard, ListMovementsPort listMovementsPort) {
        this.guard = guard;
        this.listMovementsPort = listMovementsPort;
    }

    @Override
    public List<MovementResponse> list(OrganizationId organizationId, CaseId caseId) {
        guard.requireExists(organizationId, caseId);
        return listMovementsPort.ofCase(organizationId, caseId).stream().map(MovementResponse::of).toList();
    }
}
