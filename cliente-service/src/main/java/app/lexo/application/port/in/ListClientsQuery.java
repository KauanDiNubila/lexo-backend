package app.lexo.application.port.in;

import app.lexo.common.web.Pagina;
import app.lexo.domain.OrganizationId;

public interface ListClientsQuery {

    Pagina<ClientResponse> list(OrganizationId organizationId, String search, PageParams pageParams);
}
