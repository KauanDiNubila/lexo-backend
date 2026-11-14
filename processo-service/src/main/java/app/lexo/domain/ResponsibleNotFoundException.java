package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class ResponsibleNotFoundException extends ResourceNotFoundException {

    public ResponsibleNotFoundException() {
        super("Respons\u00e1vel n\u00e3o encontrado");
    }
}
