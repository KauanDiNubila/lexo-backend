package app.lexo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeadlineStatus {

    PENDING("PENDENTE"),
    COMPLETED("CONCLUIDO"),
    MISSED("PERDIDO");

    private final String code;

    DeadlineStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static DeadlineStatus fromCode(String code) {
        for (DeadlineStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown DeadlineStatus: " + code);
    }
}
