package app.lexo.domain;

public record Identity(String userId, String organizationId, String role, String name, String email) {

    public Identity {
        requireText(userId, "userId");
        requireText(organizationId, "organizationId");
        requireText(role, "role");
        name = name == null ? "" : name;
        email = email == null ? "" : email;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
