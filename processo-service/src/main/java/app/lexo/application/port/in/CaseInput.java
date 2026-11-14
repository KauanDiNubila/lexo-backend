package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.CaseData;

final class CaseInput {

    private CaseInput() {
    }

    static void requireValid(CaseData data) {
        if (data.number() == null || data.number().isBlank()) {
            throw new InvalidDataException("N\u00famero do processo \u00e9 obrigat\u00f3rio");
        }
        if (data.status() == null) {
            throw new InvalidDataException("Status \u00e9 obrigat\u00f3rio");
        }
    }
}
