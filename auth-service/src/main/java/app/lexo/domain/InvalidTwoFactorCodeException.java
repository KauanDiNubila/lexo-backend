package app.lexo.domain;

public class InvalidTwoFactorCodeException extends RuntimeException {

    public InvalidTwoFactorCodeException() {
        super("Código de verificação inválido");
    }
}
