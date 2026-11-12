package app.lexo.adapter.in.web;

import app.lexo.domain.CaseData;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CaseRequest(
        @NotBlank(message = "Selecione um cliente") String clientId,
        @NotBlank(message = "N\u00famero do processo \u00e9 obrigat\u00f3rio") String number,
        String area,
        @NotNull CaseStatus status,
        String description,
        @JsonProperty("responsavelId") String responsibleId
) {

    CaseData toData() {
        return new CaseData(new ClientId(clientId), number, area, status, description, UserId.optional(responsibleId));
    }
}
