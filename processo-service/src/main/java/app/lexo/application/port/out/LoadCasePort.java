package app.lexo.application.port.out;

import app.lexo.domain.Case;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

import java.util.Optional;

public interface LoadCasePort {

    Optional<Case> byId(OrganizationId organizationId, CaseId caseId);
}
