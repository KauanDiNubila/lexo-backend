package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public record RegisterAuditEntryCommand(
        OrganizationId organizationId,
        String userId,
        String userName,
        String action,
        String entityType,
        String entityId,
        String description
) {
}
