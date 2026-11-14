package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.UserInvite;

import java.time.Instant;

public record InviteResponse(String id, String name, String email, Role role, Instant expiresAt,
                             Instant createdAt) {

    public static InviteResponse of(UserInvite invite) {
        return new InviteResponse(invite.id().value(), invite.name(), invite.email().value(), invite.role(),
                invite.expiresAt(), invite.createdAt());
    }
}
