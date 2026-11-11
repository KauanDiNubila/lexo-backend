package app.lexo.application.port.in;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface ListClientFeesQuery {

    List<HonorarioPortal> list(OrganizationId organizationId, ClientId clientId);
}
