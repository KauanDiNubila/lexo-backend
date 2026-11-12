package app.lexo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeadlineType {

    DEADLINE("PRAZO", 1.5),
    HEARING("AUDIENCIA", 1.2),
    MEETING("REUNIAO", 1.0),
    OTHER("OUTRO", 1.0);

    private final String code;
    private final double riskFactor;

    DeadlineType(String code, double riskFactor) {
        this.code = code;
        this.riskFactor = riskFactor;
    }

    @JsonValue
    public String code() {
        return code;
    }

    public double riskFactor() {
        return riskFactor;
    }

    @JsonCreator
    public static DeadlineType fromCode(String code) {
        for (DeadlineType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown DeadlineType: " + code);
    }
}
