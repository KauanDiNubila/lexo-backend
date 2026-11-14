package app.lexo.domain;

public enum RiskLevel {

    CRITICAL("CRITICO"),
    URGENT("URGENTE"),
    HIGH("ALTO"),
    MEDIUM("MEDIO"),
    LOW("BAIXO");

    private final String code;

    RiskLevel(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static RiskLevel forDaysRemaining(double effectiveDays) {
        if (effectiveDays <= 1) {
            return CRITICAL;
        }
        if (effectiveDays <= 3) {
            return URGENT;
        }
        if (effectiveDays <= 7) {
            return HIGH;
        }
        if (effectiveDays <= 15) {
            return MEDIUM;
        }
        return LOW;
    }
}
