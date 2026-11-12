package app.lexo.domain;

import java.time.Instant;

public record DeadlineData(CaseId caseId, String title, DeadlineType type, Instant date, String description) {

    public DeadlineData {
        description = description == null || description.isBlank() ? null : description;
    }
}
