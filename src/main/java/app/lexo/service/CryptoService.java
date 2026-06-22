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

/**
 * Cifra de segredos TOTP em repouso com AES-256-GCM, portada de lib/crypto.ts.
 * A chave NAO fica no banco — e derivada de TOTP_ENC_KEY (ou, na ausencia, do AUTH_SECRET).
 * Cada valor cifrado carrega seu proprio IV aleatorio. Segredos legados em texto puro
 * (sem o prefixo) continuam validos para nao causar lockout.
 */
@Service
public class CryptoService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(
            @Value("${lexo.totp-enc-key:}") String totpEncKey,
            @Value("${lexo.auth-secret}") String authSecret) {
        String secret = (totpEncKey != null && !totpEncKey.isBlank()) ? totpEncKey : authSecret;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "TOTP_ENC_KEY/AUTH_SECRET nao configurado — impossivel cifrar segredo TOTP");
        }
        this.key = deriveKey(secret);
    }

    private SecretKey deriveKey(String secret) {
        try {
            // Deriva 32 bytes deterministicos da chave (salt fixo; o IV por mensagem garante aleatoriedade).
            byte[] salt = "lexo-totp-kdf-v1".getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 100_000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de cifragem TOTP", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder b64 = Base64.getEncoder();
            // GCM no Java ja anexa a tag ao final do ciphertext; guardamos iv:ciphertext(+tag).
            return PREFIX + b64.encodeToString(iv) + ":" + b64.encodeToString(ct);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar segredo TOTP", e);
        }
    }

    public String decrypt(String stored) {
        // Compatibilidade: segredos legados em texto puro continuam validos.
        if (!isEncrypted(stored)) {
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
