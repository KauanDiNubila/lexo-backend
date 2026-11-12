package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public interface GetAgendaSummaryQuery {

    AgendaSummary summarize(OrganizationId organizationId);
}
