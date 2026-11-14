package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public record DisableTwoFactorCommand(OrganizationId organizationId, Actor actor, String code) {

    public DisableTwoFactorCommand {
        Input.requireNotBlank(code, "Código é obrigatório");
    }
}
