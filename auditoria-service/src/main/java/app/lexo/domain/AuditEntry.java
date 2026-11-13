package app.lexo.domain;

import java.time.Instant;

public record AuditEntry(
        AuditEntryId id,
        OrganizationId organizationId,
        String userId,
        String userName,
        String action,
        String entityType,
        String entityId,
        String description,
        Instant occurredAt
) {

    public AuditEntry {
        require(organizationId, "organizationId");
        require(userName, "userName");
        require(action, "action");
        require(description, "description");
        require(occurredAt, "occurredAt");
    }

    public static AuditEntry register(OrganizationId organizationId, String userId, String userName, String action,
                                      String entityType, String entityId, String description, Instant occurredAt) {
        return new AuditEntry(null, organizationId, userId, userName, action, entityType, entityId, description,
                occurredAt);
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
