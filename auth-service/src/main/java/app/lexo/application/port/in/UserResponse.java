package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.User;

import java.time.Instant;

public record UserResponse(String id, String name, String email, Role role, boolean totpEnabled,
                           Instant createdAt) {

    public static UserResponse of(User user) {
        return new UserResponse(user.id().value(), user.name(), user.email().value(), user.role(),
                user.twoFactorEnabled(), user.createdAt());
    }
}
