package app.lexo.application.port.in;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.OrganizationId;

public interface ListInvoicesQuery {

    PageResult<InvoiceResponse> list(OrganizationId organizationId, PageParams pageParams);
}
