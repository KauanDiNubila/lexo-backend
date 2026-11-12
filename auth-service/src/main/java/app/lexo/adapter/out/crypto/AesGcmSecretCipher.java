package app.lexo.adapter.out.crypto;

import app.lexo.application.port.out.SecretCipherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
class AesGcmSecretCipher implements SecretCipherPort {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    AesGcmSecretCipher(@Value("${lexo.totp-enc-key:}") String encryptionKey,
                       @Value("${lexo.auth-secret}") String authSecret) {
        String secret = (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : authSecret;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("TOTP_ENC_KEY/AUTH_SECRET is not configured, cannot encrypt TOTP secrets");
        }
        this.key = deriveKey(secret);
    }

    private SecretKey deriveKey(String secret) {
        try {
            byte[] salt = "lexo-totp-kdf-v1".getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 100_000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive the TOTP encryption key", e);
        }
    }

    @Override
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder base64 = Base64.getEncoder();
            return PREFIX + base64.encodeToString(iv) + ":" + base64.encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt the TOTP secret", e);
        }
    }

    @Override
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            String[] parts = stored.substring(PREFIX.length()).split(":");
            Base64.Decoder base64 = Base64.getDecoder();
            byte[] iv = base64.decode(parts[0]);
            byte[] encrypted = base64.decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt the TOTP secret", e);
        }
    }
}
