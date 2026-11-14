package app.lexo.application.port.in;

public record TwoFactorSetup(String secret, String otpauthUri) {
}
