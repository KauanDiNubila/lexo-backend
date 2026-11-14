package app.lexo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CaseStatus {

    ACTIVE("ATIVO"),
    SUSPENDED("SUSPENSO"),
    ARCHIVED("ARQUIVADO"),
    CLOSED("ENCERRADO");

    private final String code;

    CaseStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static CaseStatus fromCode(String code) {
        for (CaseStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CaseStatus: " + code);
    }
}
