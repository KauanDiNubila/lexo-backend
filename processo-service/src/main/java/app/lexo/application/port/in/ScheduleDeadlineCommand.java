package app.lexo.application.port.in;

import app.lexo.domain.DeadlineData;
import app.lexo.domain.OrganizationId;

public record ScheduleDeadlineCommand(OrganizationId organizationId, DeadlineData data) {

    public ScheduleDeadlineCommand {
        DeadlineInput.requireValid(data);
    }
}
