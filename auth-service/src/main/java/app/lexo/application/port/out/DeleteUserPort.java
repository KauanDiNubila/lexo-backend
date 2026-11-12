package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public interface DeleteUserPort {

    void delete(OrganizationId organizationId, UserId id);
}
