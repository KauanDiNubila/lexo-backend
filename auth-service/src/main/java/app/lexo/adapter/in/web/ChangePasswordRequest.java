package app.lexo.adapter.in.web;

import app.lexo.application.port.in.Actor;
import app.lexo.application.port.in.ChangePasswordCommand;
import app.lexo.domain.OrganizationId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String newPassword,
        @NotBlank String confirmPassword
) {

    ChangePasswordCommand toCommand(OrganizationId organizationId, Actor actor) {
        return new ChangePasswordCommand(organizationId, actor, currentPassword, newPassword, confirmPassword);
    }
}
