package app.lexo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public class IaDtos {

    public record PrazoCtx(
            @Size(max = 200) String titulo,
            @Size(max = 40) String data,
            @Size(max = 40) String status
    ) {
    }

    public record ResumoProcessoRequest(
            @Size(max = 60) String numero,
            @Size(max = 80) String area,
            @Size(max = 40) String status,
            @Size(max = 200) String cliente,
            @Size(max = 100, message = "Prazos demais para resumir") List<@Valid PrazoCtx> prazos
    ) {
    }

    public record ResumoResponse(String resumo, String fonte) {
    }
}
