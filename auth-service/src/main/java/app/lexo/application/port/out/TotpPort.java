package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;

public interface TotpPort {

    String generateSecret();

    boolean verify(String secret, String code);

    String otpauthUri(String secret, EmailAddress account);
}
