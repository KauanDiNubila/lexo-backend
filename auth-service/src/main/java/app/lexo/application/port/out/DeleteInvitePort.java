package app.lexo.application.port.out;

import app.lexo.domain.InviteId;
import app.lexo.domain.OrganizationId;

public interface DeleteInvitePort {

    void delete(OrganizationId organizationId, InviteId id);
}
