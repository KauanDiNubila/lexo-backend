package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AcceptInviteCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
        @NotBlank String confirmPassword
) {

    AcceptInviteCommand toCommand() {
        return new AcceptInviteCommand(token, password, confirmPassword);
    }
}
