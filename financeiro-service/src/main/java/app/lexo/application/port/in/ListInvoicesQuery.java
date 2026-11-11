package app.lexo.application.port.in;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.Pagina;
import app.lexo.domain.OrganizationId;

public interface ListInvoicesQuery {

    Pagina<InvoiceResponse> list(OrganizationId organizationId, PageParams pageParams);
}
