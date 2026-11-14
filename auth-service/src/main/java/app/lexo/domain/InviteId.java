package app.lexo.domain;

public record InviteId(String value) {

    public InviteId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("InviteId is empty");
        }
    }
}
