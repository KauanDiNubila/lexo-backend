package app.lexo.application.port.in;

public record AcceptInviteCommand(String token, String password, String confirmPassword) {

    public AcceptInviteCommand {
        Input.requireNotBlank(token, "Convite inválido");
        Input.requireMinLength(password, 8, "Senha deve ter ao menos 8 caracteres");
    }
}
