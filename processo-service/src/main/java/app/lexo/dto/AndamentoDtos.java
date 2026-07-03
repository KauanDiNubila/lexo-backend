package app.lexo.dto;

import app.lexo.domain.Andamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class AndamentoDtos {

    public record AndamentoRequest(
            @NotBlank(message = "Descreva o andamento") String title,
            String description,
            @NotNull(message = "Informe a data") Instant date
    ) {
    }

    public record AndamentoResponse(
            String id,
            String caseId,
            String title,
            String description,
            Instant date,
            Instant createdAt
    ) {
        public static AndamentoResponse from(Andamento a) {
            return new AndamentoResponse(a.getId(), a.getCaseId(), a.getTitle(),
                    a.getDescription(), a.getDate(), a.getCreatedAt());
        }
    }
}
