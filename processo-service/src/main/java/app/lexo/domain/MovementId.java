package app.lexo.domain;

public record MovementId(String value) {

    public MovementId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MovementId is empty");
        }
    }
}
