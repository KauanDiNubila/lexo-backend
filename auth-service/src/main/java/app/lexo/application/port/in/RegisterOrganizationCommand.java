package app.lexo.application.port.in;

import app.lexo.domain.EmailAddress;

public record RegisterOrganizationCommand(String organizationName, String name, EmailAddress email,
                                          String password, String confirmPassword) {

    public RegisterOrganizationCommand {
        Input.requireMinLength(organizationName, 2, "Nome do escritório muito curto");
        Input.requireMinLength(name, 2, "Nome muito curto");
        Input.requireMinLength(password, 8, "Senha deve ter ao menos 8 caracteres");
    }
}
