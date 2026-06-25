package app.lexo.dto;

import app.lexo.domain.Deadline;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.domain.enums.DeadlineType;
import app.lexo.util.RiskCalculator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class DeadlineDtos {

    private DeadlineDtos() {
    }

    public record DeadlineRequest(
            @NotBlank(message = "Selecione um processo") String caseId,
            @NotBlank(message = "Título é obrigatório") String title,
            @NotNull DeadlineType type,
            @NotNull(message = "Data é obrigatória") Instant date,
            String description
    ) {
    }

    public record StatusRequest(
            boolean completed
    ) {
    }

    public record DeadlineResponse(
            String id,
            String caseId,
            String title,
            DeadlineType type,
            DeadlineStatus status,
            Instant date,
            String description,
            String risk
    ) {
        public static DeadlineResponse from(Deadline d) {
            RiskCalculator.RiskLevel level =
                    RiskCalculator.getRiskLevel(d.getDate(), d.getType(), d.getStatus());
            return new DeadlineResponse(
                    d.getId(), d.getCaseId(), d.getTitle(), d.getType(), d.getStatus(),
                    d.getDate(), d.getDescription(), level == null ? null : level.name());
        }
    }
}
