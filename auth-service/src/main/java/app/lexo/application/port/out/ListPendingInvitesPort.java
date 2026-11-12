package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserInvite;

import java.util.List;

public interface ListPendingInvitesPort {

    List<UserInvite> of(OrganizationId organizationId);
}
