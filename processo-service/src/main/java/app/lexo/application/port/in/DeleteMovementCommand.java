package app.lexo.application.port.in;

import app.lexo.domain.CaseId;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;

public record DeleteMovementCommand(OrganizationId organizationId, CaseId caseId, MovementId movementId) {
}
