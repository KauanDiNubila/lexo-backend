package app.lexo.application.port.out;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public interface ClientExistsPort {

    boolean exists(OrganizationId organizationId, ClientId clientId);
}
