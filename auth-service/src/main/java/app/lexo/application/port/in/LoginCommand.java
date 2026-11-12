package app.lexo.application.port.in;

import app.lexo.domain.EmailAddress;

public record LoginCommand(EmailAddress email, String password, String totpCode, String clientAddress) {

    public LoginCommand {
        Input.requireNotBlank(password, "Senha é obrigatória");
    }
}
