package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;

public interface DeleteMovementPort {

    boolean delete(OrganizationId organizationId, CaseId caseId, MovementId movementId);
}
