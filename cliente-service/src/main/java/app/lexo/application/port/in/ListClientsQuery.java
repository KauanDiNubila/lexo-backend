package app.lexo.application.port.in;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.OrganizationId;

public interface ListClientsQuery {

    PageResult<ClientResponse> list(OrganizationId organizationId, String search, PageParams pageParams);
}
