package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;

final class ClientName {

    private static final int MINIMUM_LENGTH = 2;

    private ClientName() {
    }

    static void requireValid(String name) {
        if (name == null || name.isBlank() || name.length() < MINIMUM_LENGTH) {
            throw new InvalidDataException("Nome muito curto");
        }
    }
}
