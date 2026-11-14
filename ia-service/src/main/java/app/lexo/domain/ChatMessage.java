package app.lexo.domain;

public record ChatMessage(ChatRole role, String text) {

    public ChatMessage {
        if (role == null) {
            throw new IllegalArgumentException("role is empty");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is empty");
        }
    }
}
