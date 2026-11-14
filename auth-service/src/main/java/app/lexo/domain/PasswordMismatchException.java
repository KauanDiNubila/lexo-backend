package app.lexo.domain;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("As senhas não coincidem");
    }
}
