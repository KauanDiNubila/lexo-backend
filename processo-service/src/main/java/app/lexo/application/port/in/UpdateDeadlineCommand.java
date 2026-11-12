package app.lexo.application.port.in;

import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.OrganizationId;

public record UpdateDeadlineCommand(OrganizationId organizationId, DeadlineId deadlineId, DeadlineData data) {

    public UpdateDeadlineCommand {
        DeadlineInput.requireValid(data);
    }
}
