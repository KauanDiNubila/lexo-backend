package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface DetachCaseFromInvoicesPort {

    int detach(OrganizationId organizationId, CaseId caseId);
}
