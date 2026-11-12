package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface CaseExistsQuery {

    boolean exists(OrganizationId organizationId, CaseId caseId);
}
