package app.lexo.application.port.out;

import app.lexo.domain.DeadlineId;
import app.lexo.domain.OrganizationId;

public interface DeleteDeadlinePort {

    boolean delete(OrganizationId organizationId, DeadlineId deadlineId);
}
