package app.lexo.application.port.out;

import app.lexo.domain.Case;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface LoadClientCasesPort {

    List<Case> ofClient(OrganizationId organizationId, ClientId clientId);
}
