package app.lexo.application.port.in;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.OrganizationId;

public interface ListCasesQuery {

    PageResult<CaseResponse> list(OrganizationId organizationId, CaseFilter filter, PageParams pageParams);
}
