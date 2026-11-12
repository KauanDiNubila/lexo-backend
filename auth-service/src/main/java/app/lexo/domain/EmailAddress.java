package app.lexo.domain;

public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EmailAddress is empty");
        }
        value = value.trim().toLowerCase();
    }
}
