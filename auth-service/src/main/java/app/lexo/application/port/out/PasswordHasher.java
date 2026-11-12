package app.lexo.application.port.out;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);

    void consumeTime(String rawPassword);
}
