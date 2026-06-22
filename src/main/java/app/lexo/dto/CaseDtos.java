package app.lexo.dto;

import app.lexo.domain.Case;
import app.lexo.domain.enums.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class CaseDtos {

    private CaseDtos() {
    }

    public record CaseRequest(
            @NotBlank(message = "Selecione um cliente") String clientId,
            @NotBlank(message = "Número do processo é obrigatório") String number,
            String area,
            @NotNull CaseStatus status,
            String description,
            String responsavelId
    ) {
    }

    public record CaseResponse(
            String id,
            String clientId,
            String number,
            String area,
            CaseStatus status,
            String description,
            String responsavelId,
            Instant createdAt
    ) {
        public static CaseResponse from(Case c) {
            return new CaseResponse(
                    c.getId(), c.getClientId(), c.getNumber(), c.getArea(),
                    c.getStatus(), c.getDescription(), c.getResponsavelId(), c.getCreatedAt());
        }
    }
}
