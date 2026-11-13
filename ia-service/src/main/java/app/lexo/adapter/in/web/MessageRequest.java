package app.lexo.adapter.in.web;

import app.lexo.domain.ChatMessage;
import app.lexo.domain.ChatRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

record MessageRequest(
        @Size(max = 20, message = "Papel inválido") @JsonProperty("papel") String role,
        @Size(max = 8000, message = "Mensagem muito longa") @JsonProperty("texto") String text
) {

    boolean hasText() {
        return text != null && !text.isBlank();
    }

    ChatMessage toDomain() {
        return new ChatMessage("assistant".equals(role) ? ChatRole.ASSISTANT : ChatRole.USER, text);
    }
}
