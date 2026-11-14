package app.lexo.application.port.in;

import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.InvoiceData;

import java.math.BigDecimal;

final class InvoiceInput {

    private InvoiceInput() {
    }

    static void requireValid(InvoiceData data) {
        if (data.description() == null || data.description().isBlank()) {
            throw new InvalidDataException("Descri\u00e7\u00e3o \u00e9 obrigat\u00f3ria");
        }
        if (data.amount() == null || data.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("Valor deve ser maior que zero");
        }
        if (data.dueDate() == null) {
            throw new InvalidDataException("Vencimento \u00e9 obrigat\u00f3rio");
        }
    }
}
