package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class CaseNotFoundException extends ResourceNotFoundException {

    public CaseNotFoundException() {
        super("Processo n\u00e3o encontrado");
    }
}
