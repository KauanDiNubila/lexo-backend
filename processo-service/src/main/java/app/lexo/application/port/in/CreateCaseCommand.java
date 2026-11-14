package app.lexo.application.port.in;

import app.lexo.domain.CaseData;
import app.lexo.domain.OrganizationId;

public record CreateCaseCommand(OrganizationId organizationId, Actor actor, CaseData data) {

    public CreateCaseCommand {
        CaseInput.requireValid(data);
    }
}
