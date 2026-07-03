package app.lexo.dto;

import app.lexo.domain.AuditLog;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class TotpDtos {

    private TotpDtos() {
    }

    public record InitiateResponse(
            String secret,
            String otpauthUri
    ) {
    }

    public record CodeRequest(
            @NotBlank(message = "Código é obrigatório") String code
    ) {
    }

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
}
