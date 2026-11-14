package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class InvoiceNotFoundException extends ResourceNotFoundException {

    public InvoiceNotFoundException() {
        super("Honorário n\u00e3o encontrado");
    }
}
