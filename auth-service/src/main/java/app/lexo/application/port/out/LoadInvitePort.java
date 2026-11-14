package app.lexo.application.port.out;

import app.lexo.domain.InviteId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserInvite;

import java.util.Optional;

public interface LoadInvitePort {

    Optional<UserInvite> byToken(String token);

    Optional<UserInvite> inOrganization(OrganizationId organizationId, InviteId id);
}
