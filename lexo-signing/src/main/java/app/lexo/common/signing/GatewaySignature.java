package app.lexo.common.signing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class GatewaySignature {

    public static final String H_TIMESTAMP = "X-Gateway-Ts";
    public static final String H_SIGNATURE = "X-Gateway-Sig";

    public static final String DEV_KEY = "lexo-gateway-dev-signing-key-com-mais-de-32-bytes";

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = "|";
    private static final int MIN_LENGTH = 32;

    private GatewaySignature() {
    }

    public static String payload(String userId, String orgId, String role,
                                 String name, String email, long timestamp) {
        return String.join(SEPARATOR,
                orEmpty(userId), orEmpty(orgId), orEmpty(role), orEmpty(name), orEmpty(email), Long.toString(timestamp));
    }

    public static String sign(String key, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar os headers de identidade", e);
        }
    }

    public static boolean verify(String key, String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        byte[] expected = sign(key, payload).getBytes(StandardCharsets.UTF_8);
        byte[] received = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, received);
    }

    public static boolean withinWindow(long timestamp, long now, long toleranceSeconds) {
        return Math.abs(now - timestamp) <= toleranceSeconds;
    }

    public static void requireSecureKey(String key, boolean devProfile) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY ausente. Defina a variavel de ambiente GATEWAY_SIGNING_KEY "
                    + "(>= " + MIN_LENGTH + " bytes) para o gateway e para todos os servicos.");
        }
        if (key.getBytes(StandardCharsets.UTF_8).length < MIN_LENGTH) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY precisa ter ao menos " + MIN_LENGTH + " bytes.");
        }
        if (DEV_KEY.equals(key) && !devProfile) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY inseguro. Defina a variavel de ambiente GATEWAY_SIGNING_KEY, "
                    + "ou rode com o profile 'dev' para desenvolvimento local. "
                    + "O servico recusa iniciar com a chave default fora de dev.");
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
