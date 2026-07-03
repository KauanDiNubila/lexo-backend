package app.lexo.dto;

import java.util.List;

/** DTOs do assistente/chat da Lexo IA. */
public class ChatDtos {

    /** papel: "user" (advogado) ou "assistant" (IA). */
    public record Mensagem(String papel, String texto) {
    }

    /** Contexto opcional: a conversa pode ser "sobre" um processo especifico. */
    public record ProcessoContexto(String numero, String area, String status, String cliente) {
    }

    public record ChatRequest(List<Mensagem> mensagens, ProcessoContexto contexto) {
    }

    /** fonte = "gemini" (IA real) ou "mock" (IA indisponivel). */
    public record ChatResponse(String resposta, String fonte) {
    }
}
