package app.lexo.application.port.in;

import app.lexo.domain.AuditEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AuditEntryResponse(
        String id,
        String userName,
        String action,
        String entityType,
        String entityId,
        String description,
        @JsonProperty("createdAt") Instant occurredAt
) {

    public static AuditEntryResponse of(AuditEntry entry) {
        return new AuditEntryResponse(entry.id().value(), entry.userName(), entry.action(), entry.entityType(),
                entry.entityId(), entry.description(), entry.occurredAt());
    }
}
