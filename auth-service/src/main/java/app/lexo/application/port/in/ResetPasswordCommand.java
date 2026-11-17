package app.lexo.application.port.in;

public record ResetPasswordCommand(String token, String password, String confirmPassword) {

    public ResetPasswordCommand {
        Input.requireNotBlank(token, "Link inválido ou expirado");
        Input.requireMinLength(password, 8, "Senha deve ter ao menos 8 caracteres");
    }
}
