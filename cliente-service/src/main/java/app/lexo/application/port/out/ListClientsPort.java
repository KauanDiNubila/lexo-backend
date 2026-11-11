package app.lexo.application.port.out;

import app.lexo.application.port.in.PageParams;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Client;
import app.lexo.domain.OrganizationId;

public interface ListClientsPort {

    Pagina<Client> list(OrganizationId organizationId, String search, PageParams pageParams);
}
