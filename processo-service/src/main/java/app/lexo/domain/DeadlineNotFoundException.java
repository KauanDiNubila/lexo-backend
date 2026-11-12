package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class DeadlineNotFoundException extends ResourceNotFoundException {

    public DeadlineNotFoundException() {
        super("Prazo n\u00e3o encontrado");
    }
}
