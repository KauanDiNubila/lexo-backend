package app.lexo.domain;

public class UnknownSessionUserException extends RuntimeException {

    public UnknownSessionUserException() {
        super("Usuário não encontrado");
    }
}
