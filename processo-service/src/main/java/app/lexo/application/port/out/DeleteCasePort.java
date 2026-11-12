package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface DeleteCasePort {

    void delete(OrganizationId organizationId, CaseId caseId);
}
