package app.lexo.application.port.out;

import app.lexo.application.port.in.ClientOption;
import app.lexo.domain.OrganizationId;
import java.util.List;

public interface ListClientOptionsPort {

    List<ClientOption> options(OrganizationId organizationId);
}
