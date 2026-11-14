package app.lexo.application.port.out;

public interface SecretCipherPort {

    String encrypt(String plain);

    String decrypt(String stored);
}
