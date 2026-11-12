package app.lexo.adapter.in.web;

import app.lexo.application.port.in.LoginCommand;
import app.lexo.domain.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank @Email(message = "Email inválido") String email,
        @NotBlank String password,
        String totpCode
) {

    LoginCommand toCommand(String clientAddress) {
        return new LoginCommand(new EmailAddress(email), password, totpCode, clientAddress);
    }
}
