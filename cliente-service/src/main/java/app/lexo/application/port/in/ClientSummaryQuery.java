package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public interface ClientSummaryQuery {

    ClientSummary summarize(OrganizationId organizationId);
}
