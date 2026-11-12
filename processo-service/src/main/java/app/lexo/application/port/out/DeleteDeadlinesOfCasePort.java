package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface DeleteDeadlinesOfCasePort {

    void deleteOfCase(OrganizationId organizationId, CaseId caseId);
}
