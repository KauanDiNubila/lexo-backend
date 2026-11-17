package app.lexo.application.port.in;

public record ResetPasswordCommand(String token, String password, String confirmPassword) {

    public static final String INVALID_LINK = "Link inválido ou expirado";

    public ResetPasswordCommand {
        Input.requireNotBlank(token, INVALID_LINK);
        Input.requireMinLength(password, 8, "Senha deve ter ao menos 8 caracteres");
    }
}
