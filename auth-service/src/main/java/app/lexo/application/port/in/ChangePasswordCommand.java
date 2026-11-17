package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public record ChangePasswordCommand(OrganizationId organizationId, Actor actor, String currentPassword,
                                    String newPassword, String confirmPassword) {

    public ChangePasswordCommand {
        Input.requireNotBlank(currentPassword, "Informe a senha atual");
        Input.requireMinLength(newPassword, 8, "Senha deve ter ao menos 8 caracteres");
    }
}
