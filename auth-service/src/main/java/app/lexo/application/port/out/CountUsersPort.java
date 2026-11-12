package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;

public interface CountUsersPort {

    int countInOrganization(OrganizationId organizationId);
}
