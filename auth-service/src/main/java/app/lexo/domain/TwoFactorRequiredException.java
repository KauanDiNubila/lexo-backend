package app.lexo.domain;

public class TwoFactorRequiredException extends RuntimeException {

    public TwoFactorRequiredException() {
        super("Código de verificação em dois fatores obrigatório");
    }
}
