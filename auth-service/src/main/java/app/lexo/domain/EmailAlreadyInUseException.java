package app.lexo.domain;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("Já existe um usuário com este email");
    }
}
