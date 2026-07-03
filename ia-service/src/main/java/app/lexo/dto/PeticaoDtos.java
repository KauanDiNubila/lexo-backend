package app.lexo.dto;

/** DTOs da geracao de rascunho de peticao/documento juridico. */
public class PeticaoDtos {

    /** Contexto opcional do processo ao qual a peca se refere. */
    public record ProcessoContexto(String numero, String area, String status, String cliente) {
    }

    /** pedido: descricao livre do que gerar (ex.: "peticao inicial de cobranca de honorarios"). */
    public record PeticaoRequest(String pedido, ProcessoContexto contexto) {
    }

    /** fonte = "gemini" (IA real) ou "mock" (modelo generico). */
    public record PeticaoResponse(String texto, String fonte) {
    }
}
