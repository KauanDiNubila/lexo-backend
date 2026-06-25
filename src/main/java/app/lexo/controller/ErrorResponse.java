package app.lexo.controller;

import java.time.Instant;

/**
 * Corpo padrao de erro da API. Mantem o campo "error" (mensagem amigavel) para
 * compatibilidade com o contrato original e adiciona contexto util ao cliente:
 * momento, status HTTP e caminho da requisicao.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String path
) {
    public static ErrorResponse of(int status, String error, String path) {
        return new ErrorResponse(Instant.now(), status, error, path);
    }
}
