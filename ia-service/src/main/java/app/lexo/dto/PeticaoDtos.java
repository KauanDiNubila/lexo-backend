package app.lexo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PeticaoDtos {

    public record PeticaoRequest(
            @NotBlank(message = "Descreva o que deve ser redigido")
            @Size(max = 4000, message = "Pedido muito longo") String pedido,
            @Valid ProcessoContexto contexto
    ) {
    }

    public record PeticaoResponse(String texto, String fonte) {
    }
}
