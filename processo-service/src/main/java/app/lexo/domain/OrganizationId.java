package app.lexo.domain;

public record OrganizationId(String value) {

    public OrganizationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrganizationId is empty");
        }
    }
}
