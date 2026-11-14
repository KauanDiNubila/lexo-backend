package app.lexo.application.port.in;

import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record CaseResponse(
        String id,
        String clientId,
        String number,
        String area,
        CaseStatus status,
        String description,
        @JsonProperty("responsavelId") String responsibleId,
        Instant createdAt
) {

    public static CaseResponse of(Case value) {
        CaseData data = value.data();
        return new CaseResponse(
                value.id().value(),
                data.clientId().value(),
                data.number(),
                data.area(),
                data.status(),
                data.description(),
                data.responsibleId() == null ? null : data.responsibleId().value(),
                value.createdAt());
    }
}
