package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public interface GetClientQuery {

    ClientResponse get(OrganizationId organizationId, ClientId clientId);
}
