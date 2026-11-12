package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.MovementData;
import app.lexo.domain.OrganizationId;

public record RecordMovementCommand(OrganizationId organizationId, CaseId caseId, MovementData data) {

    public RecordMovementCommand {
        MovementInput.requireValid(data);
    }
}
