package app.lexo.application.port.out;

import app.lexo.domain.AuditEntry;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface LoadLatestAuditEntriesPort {

    List<AuditEntry> latest(OrganizationId organizationId, int limit);
}
