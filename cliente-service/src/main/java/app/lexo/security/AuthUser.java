package app.lexo.security;

import app.lexo.domain.enums.Role;

public record AuthUser(
        String id,
        String organizationId,
        String name,
        String email,
        Role role
) {
}
