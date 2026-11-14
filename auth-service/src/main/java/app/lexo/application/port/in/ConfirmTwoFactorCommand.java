package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public record ConfirmTwoFactorCommand(OrganizationId organizationId, Actor actor, String code) {

    public ConfirmTwoFactorCommand {
        Input.requireNotBlank(code, "Código é obrigatório");
    }
}
