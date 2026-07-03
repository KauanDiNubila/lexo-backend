package app.lexo.dto;

import java.util.List;

public class ChatDtos {

    public record Mensagem(String papel, String texto) {
    }

    public record ProcessoContexto(String numero, String area, String status, String cliente) {
    }

    public record ChatRequest(List<Mensagem> mensagens, ProcessoContexto contexto) {
    }

    public record ChatResponse(String resposta, String fonte) {
    }
}
