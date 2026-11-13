package app.lexo.adapter.in.web;

import app.lexo.application.port.in.DraftPetitionCommand;
import app.lexo.domain.OrganizationId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Optional;

record DraftRequest(
        @NotBlank(message = "Descreva o que deve ser redigido")
        @Size(max = 4000, message = "Pedido muito longo") @JsonProperty("pedido") String instruction,
        @Valid @JsonProperty("contexto") CaseContextRequest context
) {

    DraftPetitionCommand toCommand(OrganizationId organizationId) {
        return new DraftPetitionCommand(organizationId, instruction,
                Optional.ofNullable(context).map(CaseContextRequest::toDomain));
    }
}
