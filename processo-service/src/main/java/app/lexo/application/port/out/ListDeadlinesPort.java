package app.lexo.application.port.out;

import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.OrganizationId;

public interface ListDeadlinesPort {

    PageResult<Deadline> list(OrganizationId organizationId, CaseId caseId, PageParams pageParams);
}
