package app.lexo.dto;

import java.util.List;

public class IaDtos {

    public record PrazoCtx(String titulo, String data, String status) {
    }

    public record ResumoProcessoRequest(
            String numero,
            String area,
            String status,
            String cliente,
            List<PrazoCtx> prazos
    ) {
    }

    public record ResumoResponse(String resumo, String fonte) {
    }
}
