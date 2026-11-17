package app.lexo.adapter.in.web;

import app.lexo.application.port.in.RequestPasswordResetCommand;
import app.lexo.domain.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record ForgotPasswordRequest(@NotBlank @Email(message = "Email inválido") String email) {

    RequestPasswordResetCommand toCommand() {
        return new RequestPasswordResetCommand(new EmailAddress(email));
    }
}
