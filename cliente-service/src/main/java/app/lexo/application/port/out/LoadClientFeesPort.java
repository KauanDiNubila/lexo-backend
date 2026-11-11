package app.lexo.application.port.out;

import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface LoadClientFeesPort {

    List<PortalFee> load(OrganizationId organizationId, ClientId clientId);
}
