package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.User;

public record UserInfo(String id, String name, String email, Role role, String organizationId) {

    public static UserInfo of(User user) {
        return new UserInfo(user.id().value(), user.name(), user.email().value(), user.role(),
                user.organizationId().value());
    }
}
