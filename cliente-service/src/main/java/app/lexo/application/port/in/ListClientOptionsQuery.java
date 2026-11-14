package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import java.util.List;

public interface ListClientOptionsQuery {

    List<ClientOption> listOptions(OrganizationId organizationId);
}
