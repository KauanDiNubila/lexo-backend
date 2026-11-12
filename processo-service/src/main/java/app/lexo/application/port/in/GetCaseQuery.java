package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface GetCaseQuery {

    CaseResponse get(OrganizationId organizationId, CaseId caseId);
}
