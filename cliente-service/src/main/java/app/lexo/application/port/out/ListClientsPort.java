package app.lexo.application.port.out;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Client;
import app.lexo.domain.OrganizationId;

public interface ListClientsPort {

    Pagina<Client> list(OrganizationId organizationId, String search, PageParams pageParams);
}
