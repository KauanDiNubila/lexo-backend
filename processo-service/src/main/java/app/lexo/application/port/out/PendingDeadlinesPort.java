package app.lexo.application.port.out;

import app.lexo.domain.Deadline;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.List;

public interface PendingDeadlinesPort {

    long countBetween(OrganizationId organizationId, Instant from, Instant to);

    long countBefore(OrganizationId organizationId, Instant limit);

    List<Deadline> next(OrganizationId organizationId, int limit);
}
