package app.lexo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CriptografiaService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CriptografiaService(
            @Value("${lexo.totp-enc-key:}") String totpEncKey,
            @Value("${lexo.auth-secret}") String authSecret) {
        String secret = (totpEncKey != null && !totpEncKey.isBlank()) ? totpEncKey : authSecret;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "TOTP_ENC_KEY/AUTH_SECRET nao configurado — impossivel cifrar segredo TOTP");
        }
        this.key = derivarChave(secret);
    }

    private SecretKey derivarChave(String secret) {
        try {

            byte[] salt = "lexo-totp-kdf-v1".getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 100_000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de cifragem TOTP", e);
        }
    }

    public boolean estaCifrado(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String cifrar(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder b64 = Base64.getEncoder();

            return PREFIX + b64.encodeToString(iv) + ":" + b64.encodeToString(ct);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar segredo TOTP", e);
        }
    }

    public String decifrar(String stored) {

        if (!estaCifrado(stored)) {
            return stored;
        }
        try {
            String[] parts = stored.substring(PREFIX.length()).split(":");
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] iv = b64.decode(parts[0]);
            byte[] ct = b64.decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar segredo TOTP", e);
        }
    }
}
