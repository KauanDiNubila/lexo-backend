package app.lexo.application.port.out;

import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.Optional;

public interface LoadClientPort {

    Optional<Client> byId(OrganizationId organizationId, ClientId clientId);

    Optional<Client> byPortalLink(String token);
}
