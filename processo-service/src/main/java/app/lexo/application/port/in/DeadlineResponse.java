package app.lexo.application.port.in;

import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.RiskLevel;

import java.time.Instant;

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

    public static DeadlineResponse of(Deadline deadline, Instant now) {
        DeadlineData data = deadline.data();
        return new DeadlineResponse(
                deadline.id().value(),
                data.caseId().value(),
                data.title(),
                data.type(),
                deadline.status(),
                data.date(),
                data.description(),
                deadline.riskAt(now).map(RiskLevel::code).orElse(null));
    }
}
