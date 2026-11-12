package app.lexo.adapter.in.web;

import app.lexo.application.port.in.RegisterOrganizationCommand;
import app.lexo.domain.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterRequest(
        @NotBlank @Size(min = 2, message = "Nome do escritório muito curto") String organizationName,
        @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
        @NotBlank @Email(message = "Email inválido") String email,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
        @NotBlank String confirmPassword
) {

    RegisterOrganizationCommand toCommand() {
        return new RegisterOrganizationCommand(organizationName, name, new EmailAddress(email), password,
                confirmPassword);
    }
}
