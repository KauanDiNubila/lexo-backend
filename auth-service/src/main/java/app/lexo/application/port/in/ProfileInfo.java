package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.User;

public record ProfileInfo(String id, String name, String email, Role role, boolean totpEnabled) {

    public static ProfileInfo of(User user) {
        return new ProfileInfo(user.id().value(), user.name(), user.email().value(), user.role(),
                user.twoFactorEnabled());
    }
}
