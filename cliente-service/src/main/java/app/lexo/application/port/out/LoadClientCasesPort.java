package app.lexo.application.port.out;

import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface LoadClientCasesPort {

    List<ProcessoPortal> load(OrganizationId organizationId, ClientId clientId);
}
