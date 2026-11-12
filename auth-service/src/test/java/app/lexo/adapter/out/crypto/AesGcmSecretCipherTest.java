package app.lexo.adapter.out.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AesGcmSecretCipher")
class AesGcmSecretCipherTest {

    private static final String AUTH_SECRET = "auth-secret-with-more-than-thirty-two-bytes";

    private AesGcmSecretCipher cipher(String encryptionKey, String authSecret) {
        return new AesGcmSecretCipher(encryptionKey, authSecret);
    }

    @Test
    @DisplayName("decrypting what was encrypted gives the original secret")
    void roundTrip() {
        AesGcmSecretCipher cipher = cipher("", AUTH_SECRET);

        String stored = cipher.encrypt("JBSWY3DPEHPK3PXP");

        assertThat(stored).startsWith("enc:v1:").doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(stored)).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    @DisplayName("every encryption uses a new random IV")
    void usesARandomIv() {
        AesGcmSecretCipher cipher = cipher("", AUTH_SECRET);

        assertThat(cipher.encrypt("SECRET")).isNotEqualTo(cipher.encrypt("SECRET"));
    }

    @Test
    @DisplayName("a value stored before encryption existed is returned as it is")
    void legacyPlainValuePassesThrough() {
        AesGcmSecretCipher cipher = cipher("", AUTH_SECRET);

        assertThat(cipher.decrypt("JBSWY3DPEHPK3PXP")).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("tampered data cannot be decrypted")
    void tamperedDataFails() {
        AesGcmSecretCipher cipher = cipher("", AUTH_SECRET);
        String stored = cipher.encrypt("SECRET");
        String tampered = stored.substring(0, stored.length() - 6) + "AAAAAA";

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a different key cannot decrypt the value")
    void differentKeyFails() {
        String stored = cipher("", AUTH_SECRET).encrypt("SECRET");

        assertThatThrownBy(() -> cipher("", "another-secret-with-more-than-thirty-two-bytes").decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("the dedicated encryption key wins over the auth secret")
    void dedicatedKeyWins() {
        String stored = cipher("dedicated-key-for-totp", "first-auth-secret-with-enough-bytes-here").encrypt("SECRET");

        assertThat(cipher("dedicated-key-for-totp", "second-auth-secret-with-enough-bytes-here").decrypt(stored))
                .isEqualTo("SECRET");
    }
}
