package app.lexo.application.port.in;

import app.lexo.domain.CaseMovement;
import app.lexo.domain.MovementData;

import java.time.Instant;

public record MovementResponse(
        String id,
        String caseId,
        String title,
        String description,
        Instant date,
        Instant createdAt
) {

    public static MovementResponse of(CaseMovement movement) {
        MovementData data = movement.data();
        return new MovementResponse(
                movement.id().value(),
                movement.caseId().value(),
                data.title(),
                data.description(),
                data.date(),
                movement.createdAt());
    }
}
