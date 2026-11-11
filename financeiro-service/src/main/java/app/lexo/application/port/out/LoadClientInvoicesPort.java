package app.lexo.application.port.out;

import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface LoadClientInvoicesPort {

    List<Invoice> ofClient(OrganizationId organizationId, ClientId clientId);
}
