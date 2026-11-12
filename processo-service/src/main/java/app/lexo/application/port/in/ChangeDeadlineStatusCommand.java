package app.lexo.application.port.in;

import app.lexo.domain.DeadlineId;
import app.lexo.domain.OrganizationId;

public record ChangeDeadlineStatusCommand(OrganizationId organizationId, DeadlineId deadlineId, boolean completed) {
}
