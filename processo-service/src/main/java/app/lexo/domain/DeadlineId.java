package app.lexo.domain;

public record DeadlineId(String value) {

    public DeadlineId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeadlineId is empty");
        }
    }
}
