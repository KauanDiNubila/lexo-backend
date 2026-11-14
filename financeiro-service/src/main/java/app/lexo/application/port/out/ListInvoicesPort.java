package app.lexo.application.port.out;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Invoice;
import app.lexo.domain.OrganizationId;

public interface ListInvoicesPort {

    PageResult<Invoice> list(OrganizationId organizationId, PageParams pageParams);
}
