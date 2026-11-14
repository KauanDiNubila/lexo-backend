package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.MovementData;

final class MovementInput {

    private MovementInput() {
    }

    static void requireValid(MovementData data) {
        if (data.title() == null || data.title().isBlank()) {
            throw new InvalidDataException("Descreva o andamento");
        }
        if (data.date() == null) {
            throw new InvalidDataException("Informe a data");
        }
    }
}
