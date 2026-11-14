package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public record ChangeUserRoleCommand(OrganizationId organizationId, Actor actor, UserId targetId, Role role) {

    public ChangeUserRoleCommand {
        Input.requireNotNull(role, "Papel é obrigatório");
    }
}
