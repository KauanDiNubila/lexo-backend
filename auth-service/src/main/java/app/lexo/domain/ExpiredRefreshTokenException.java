package app.lexo.domain;

public class ExpiredRefreshTokenException extends RuntimeException {

    public ExpiredRefreshTokenException() {
        super("Sessão expirada. Faça login novamente.");
    }
}
