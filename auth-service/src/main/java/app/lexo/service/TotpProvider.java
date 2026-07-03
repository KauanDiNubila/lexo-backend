package app.lexo.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TotpProvider {

    private static final String ISSUER = "Lexo";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verify(String secret, String code) {
        if (secret == null || code == null) return false;
        return codeVerifier.isValidCode(secret, code.trim());
    }

    public String otpauthUri(String secret, String accountEmail) {
        String label = URLEncoder.encode(ISSUER + ":" + accountEmail, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }
}
