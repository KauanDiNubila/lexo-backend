package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public record DetachCaseCommand(OrganizationId organizationId, CaseId caseId) {
}
