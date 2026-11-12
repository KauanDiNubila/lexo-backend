package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface CaseExistsPort {

    boolean exists(OrganizationId organizationId, CaseId caseId);
}
