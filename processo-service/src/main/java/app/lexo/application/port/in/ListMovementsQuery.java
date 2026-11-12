package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface ListMovementsQuery {

    List<MovementResponse> list(OrganizationId organizationId, CaseId caseId);
}
