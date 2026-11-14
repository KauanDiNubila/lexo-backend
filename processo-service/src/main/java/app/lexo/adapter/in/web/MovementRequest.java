package app.lexo.adapter.in.web;

import app.lexo.domain.MovementData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

record MovementRequest(
        @NotBlank(message = "Descreva o andamento") String title,
        String description,
        @NotNull(message = "Informe a data") Instant date
) {

    MovementData toData() {
        return new MovementData(title, description, date);
    }
}
