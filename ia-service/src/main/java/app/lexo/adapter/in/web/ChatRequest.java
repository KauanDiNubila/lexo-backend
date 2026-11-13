package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ChatCommand;
import app.lexo.domain.OrganizationId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Optional;

record ChatRequest(
        @NotEmpty(message = "Envie ao menos uma mensagem")
        @Size(max = 30, message = "Conversa muito longa")
        @JsonProperty("mensagens") List<@Valid MessageRequest> messages,
        @Valid @JsonProperty("contexto") CaseContextRequest context
) {

    ChatCommand toCommand(OrganizationId organizationId) {
        return new ChatCommand(organizationId,
                messages.stream().filter(MessageRequest::hasText).map(MessageRequest::toDomain).toList(),
                Optional.ofNullable(context).map(CaseContextRequest::toDomain));
    }
}
