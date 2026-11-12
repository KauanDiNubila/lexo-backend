package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;

final class Input {

    private Input() {
    }

    static void requireMinLength(String value, int min, String message) {
        if (value == null || value.isBlank() || value.length() < min) {
            throw new InvalidDataException(message);
        }
    }

    static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(message);
        }
    }

    static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new InvalidDataException(message);
        }
    }
}
