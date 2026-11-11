package app.lexo.application.port.out;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface LoadClientFeesPort {

    List<HonorarioPortal> load(OrganizationId organizationId, ClientId clientId);
}
