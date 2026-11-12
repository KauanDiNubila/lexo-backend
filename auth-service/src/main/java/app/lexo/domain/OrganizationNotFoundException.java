package app.lexo.domain;

import app.lexo.common.error.ResourceNotFoundException;

public class OrganizationNotFoundException extends ResourceNotFoundException {

    public OrganizationNotFoundException() {
        super("Escritório não encontrado");
    }
}
