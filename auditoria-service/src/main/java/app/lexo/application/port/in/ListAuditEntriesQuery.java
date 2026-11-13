package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

import java.util.List;

public interface ListAuditEntriesQuery {

    List<AuditEntryResponse> latest(OrganizationId organizationId);
}
