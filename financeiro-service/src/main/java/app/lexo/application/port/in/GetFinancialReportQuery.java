package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import java.time.Instant;

public interface GetFinancialReportQuery {

    FinancialReport report(OrganizationId organizationId, Instant start, Instant end);
}
