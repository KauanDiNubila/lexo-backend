package app.lexo.application.port.in;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;

public interface ListDeadlinesQuery {

    PageResult<DeadlineResponse> list(OrganizationId organizationId, CaseId caseId, PageParams pageParams);
}
