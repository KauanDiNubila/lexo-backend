package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface DeleteMovementsOfCasePort {

    void deleteOfCase(OrganizationId organizationId, CaseId caseId);
}
