package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

import java.time.Instant;

public record RegisterAuditEntryCommand(
        OrganizationId organizationId,
        String userId,
        String userName,
        String action,
        String entityType,
        String entityId,
        String description,
        Instant occurredAt
) {
}
