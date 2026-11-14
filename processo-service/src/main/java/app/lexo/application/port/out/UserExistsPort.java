package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public interface UserExistsPort {

    boolean exists(OrganizationId organizationId, UserId userId);
}
