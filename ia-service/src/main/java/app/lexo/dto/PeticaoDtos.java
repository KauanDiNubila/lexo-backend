package app.lexo.dto;

public class PeticaoDtos {

    public record ProcessoContexto(String numero, String area, String status, String cliente) {
    }

    public record PeticaoRequest(String pedido, ProcessoContexto contexto) {
    }

    public record PeticaoResponse(String texto, String fonte) {
    }
}
