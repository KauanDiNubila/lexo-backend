package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public interface ClientExistsQuery {

    boolean exists(OrganizationId organizationId, ClientId clientId);
}
