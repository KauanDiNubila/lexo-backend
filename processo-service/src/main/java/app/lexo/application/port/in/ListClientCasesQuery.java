package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface ListClientCasesQuery {

    List<CaseOverview> list(OrganizationId organizationId, ClientId clientId);
}
