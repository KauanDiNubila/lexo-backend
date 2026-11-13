package app.lexo.domain;

public record AuditEntryId(String value) {

    public AuditEntryId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AuditEntryId is empty");
        }
    }
}
