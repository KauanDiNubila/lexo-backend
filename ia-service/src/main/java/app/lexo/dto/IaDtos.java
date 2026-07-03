package app.lexo.dto;

import java.util.List;

/** DTOs do ia-service. */
public class IaDtos {

    public record PrazoCtx(String titulo, String data, String status) {
    }

    /** Contexto do processo enviado pelo frontend para gerar o resumo. */
    public record ResumoProcessoRequest(
            String numero,
            String area,
            String status,
            String cliente,
            List<PrazoCtx> prazos
    ) {
    }

    /** fonte = "gemini" (IA real) ou "mock" (resumo heuristico, sem chave). */
    public record ResumoResponse(String resumo, String fonte) {
    }
}
