package app.lexo.application.port.in;

import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public record UpdateCaseCommand(OrganizationId organizationId, CaseId caseId, Actor actor, CaseData data) {

    public UpdateCaseCommand {
        CaseInput.requireValid(data);
    }
}
