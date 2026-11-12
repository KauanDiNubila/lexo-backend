package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class MovementNotFoundException extends ResourceNotFoundException {

    public MovementNotFoundException() {
        super("Andamento n\u00e3o encontrado");
    }
}
