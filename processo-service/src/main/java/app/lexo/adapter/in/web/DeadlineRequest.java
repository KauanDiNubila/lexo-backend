package app.lexo.adapter.in.web;

import app.lexo.domain.CaseId;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

record DeadlineRequest(
        @NotBlank(message = "Selecione um processo") String caseId,
        @NotBlank(message = "T\u00edtulo \u00e9 obrigat\u00f3rio") String title,
        @NotNull DeadlineType type,
        @NotNull(message = "Data \u00e9 obrigat\u00f3ria") Instant date,
        String description
) {

    DeadlineData toData() {
        return new DeadlineData(new CaseId(caseId), title, type, date, description);
    }
}
