package app.lexo.application.port.out;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Client;
import app.lexo.domain.OrganizationId;

public interface ListClientsPort {

    PageResult<Client> list(OrganizationId organizationId, String search, PageParams pageParams);
}
