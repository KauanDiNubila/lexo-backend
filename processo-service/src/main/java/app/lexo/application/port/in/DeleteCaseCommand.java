package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public record DeleteCaseCommand(OrganizationId organizationId, CaseId caseId, Actor actor) {
}
