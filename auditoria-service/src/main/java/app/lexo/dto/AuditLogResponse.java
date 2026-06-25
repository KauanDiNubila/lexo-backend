package app.lexo.dto;

import app.lexo.domain.AuditLog;

import java.time.Instant;

/** Resposta de leitura do log de auditoria. */
public record AuditLogResponse(
        String id,
        String userName,
        String action,
        String entityType,
        String entityId,
        String description,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(), a.getUserName(), a.getAction(), a.getEntityType(),
                a.getEntityId(), a.getDescription(), a.getCreatedAt());
    }
}
