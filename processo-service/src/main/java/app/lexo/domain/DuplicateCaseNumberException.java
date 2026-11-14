package app.lexo.domain;

import app.lexo.common.error.ConflictException;

public class DuplicateCaseNumberException extends ConflictException {

    public DuplicateCaseNumberException() {
        super("J\u00e1 existe um processo com este n\u00famero");
    }
}
