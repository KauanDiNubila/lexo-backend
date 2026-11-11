package app.lexo.domain;

public record CaseId(String value) {

    public CaseId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CaseId is empty");
        }
    }

    public static CaseId optional(String value) {
        return value == null || value.isBlank() ? null : new CaseId(value);
    }
}
