package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class ClientNotFoundException extends ResourceNotFoundException {

    public ClientNotFoundException() {
        super("Cliente não encontrado");
    }
}
