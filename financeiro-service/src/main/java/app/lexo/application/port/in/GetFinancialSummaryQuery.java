package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public interface GetFinancialSummaryQuery {

    FinancialSummary summarize(OrganizationId organizationId);
}
