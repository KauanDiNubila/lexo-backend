package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public interface GetCaseSummaryQuery {

    CaseSummary summarize(OrganizationId organizationId);
}
