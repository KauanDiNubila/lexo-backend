package app.lexo.domain;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId is empty");
        }
    }

    public static UserId optional(String value) {
        return value == null || value.isBlank() ? null : new UserId(value);
    }
}
