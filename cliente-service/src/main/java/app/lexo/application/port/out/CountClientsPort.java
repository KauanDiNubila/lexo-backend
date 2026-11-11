package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;

public interface CountClientsPort {

    long count(OrganizationId organizationId);
}
