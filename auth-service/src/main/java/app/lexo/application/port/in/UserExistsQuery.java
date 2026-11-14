package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public interface UserExistsQuery {

    boolean exists(OrganizationId organizationId, UserId userId);
}
