package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException() {
        super("Usuário não encontrado");
    }
}
