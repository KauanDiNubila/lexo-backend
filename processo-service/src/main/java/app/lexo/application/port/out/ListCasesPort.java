package app.lexo.application.port.out;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Case;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.OrganizationId;

public interface ListCasesPort {

    PageResult<Case> list(OrganizationId organizationId, CaseFilter filter, PageParams pageParams);
}
