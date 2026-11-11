package app.lexo.application.port.out;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public interface DeleteClientInvoicesPort {

    int deleteOfClient(OrganizationId organizationId, ClientId clientId);
}
