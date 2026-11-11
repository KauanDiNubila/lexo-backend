package app.lexo.domain;

public record ClientId(String value) {

    public ClientId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClientId is empty");
        }
    }
}
