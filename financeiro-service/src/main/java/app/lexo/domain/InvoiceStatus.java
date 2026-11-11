package app.lexo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InvoiceStatus {
    PENDING("PENDENTE"),
    PAID("PAGO"),
    OVERDUE("ATRASADO"),
    CANCELLED("CANCELADO");

    private final String code;

    InvoiceStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static InvoiceStatus fromCode(String code) {
        for (InvoiceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invoice status: " + code);
    }
}
