package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ResetPasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
        @NotBlank String confirmPassword
) {

    ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(token, password, confirmPassword);
    }
}
