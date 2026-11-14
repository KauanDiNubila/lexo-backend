package app.lexo.application.port.in;

import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;

public record InviteUserCommand(OrganizationId organizationId, Actor actor, String name, EmailAddress email,
                                Role role) {

    public InviteUserCommand {
        Input.requireMinLength(name, 2, "Nome muito curto");
        Input.requireNotNull(role, "Papel é obrigatório");
    }
}
