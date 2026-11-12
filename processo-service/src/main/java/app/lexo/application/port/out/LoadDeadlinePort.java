package app.lexo.application.port.out;

import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.OrganizationId;

import java.util.Optional;

public interface LoadDeadlinePort {

    Optional<Deadline> byId(OrganizationId organizationId, DeadlineId deadlineId);
}
