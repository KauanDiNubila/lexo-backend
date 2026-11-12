package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;

import java.util.List;

public interface ListUsersPort {

    List<User> inOrganization(OrganizationId organizationId);
}
