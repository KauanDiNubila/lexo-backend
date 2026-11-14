package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.DeadlineData;

final class DeadlineInput {

    private DeadlineInput() {
    }

    static void requireValid(DeadlineData data) {
        if (data.title() == null || data.title().isBlank()) {
            throw new InvalidDataException("T\u00edtulo \u00e9 obrigat\u00f3rio");
        }
        if (data.type() == null) {
            throw new InvalidDataException("Tipo \u00e9 obrigat\u00f3rio");
        }
        if (data.date() == null) {
            throw new InvalidDataException("Data \u00e9 obrigat\u00f3ria");
        }
    }
}
