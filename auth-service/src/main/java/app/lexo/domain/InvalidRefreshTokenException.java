package app.lexo.domain;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Sessão inválida. Faça login novamente.");
    }
}
