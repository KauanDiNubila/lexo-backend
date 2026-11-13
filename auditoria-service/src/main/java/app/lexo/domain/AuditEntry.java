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
        Instant createdAt
) {

    public AuditEntry {
        require(organizationId, "organizationId");
        require(userName, "userName");
        require(action, "action");
        require(description, "description");
    }

    public static AuditEntry register(OrganizationId organizationId, String userId, String userName, String action,
                                      String entityType, String entityId, String description) {
        return new AuditEntry(null, organizationId, userId, userName, action, entityType, entityId, description, null);
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
