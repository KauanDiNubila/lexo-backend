package app.lexo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ChatDtos {

    public record Mensagem(
            @Size(max = 20, message = "Papel inválido") String papel,
            @Size(max = 8000, message = "Mensagem muito longa") String texto
    ) {
    }

    public record ChatRequest(
            @NotEmpty(message = "Envie ao menos uma mensagem")
            @Size(max = 30, message = "Conversa muito longa")
            List<@Valid Mensagem> mensagens,
            @Valid ProcessoContexto contexto
    ) {
    }

    public record ChatResponse(String resposta, String fonte) {
    }
}
