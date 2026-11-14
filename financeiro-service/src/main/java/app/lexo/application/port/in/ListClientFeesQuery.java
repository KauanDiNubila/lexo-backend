package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface ListClientFeesQuery {

    List<InvoiceResponse> list(OrganizationId organizationId, ClientId clientId);
}
