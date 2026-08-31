package app.lexo.common.security;

public record AuthUser(
        String id,
        String organizationId,
        String name,
        String email,
        Role role
) {
}
