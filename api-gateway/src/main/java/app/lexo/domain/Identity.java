package app.lexo.domain;

public record Identity(String userId, String organizationId, String role, String name, String email) {

    public Identity {
        name = name == null ? "" : name;
        email = email == null ? "" : email;
    }
}
