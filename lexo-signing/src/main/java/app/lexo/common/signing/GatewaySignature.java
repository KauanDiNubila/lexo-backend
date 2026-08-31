package app.lexo.common.signing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class GatewaySignature {

    public static final String H_TIMESTAMP = "X-Gateway-Ts";
    public static final String H_ASSINATURA = "X-Gateway-Sig";

    public static final String CHAVE_DEV = "lexo-gateway-dev-signing-key-com-mais-de-32-bytes";

    private static final String ALGORITMO = "HmacSHA256";
    private static final String SEPARADOR = "|";
    private static final int TAMANHO_MINIMO = 32;

    private GatewaySignature() {
    }

    public static String payload(String userId, String orgId, String role,
                                 String nome, String email, long timestamp) {
        return String.join(SEPARADOR,
                nvl(userId), nvl(orgId), nvl(role), nvl(nome), nvl(email), Long.toString(timestamp));
    }

    public static String sign(String chave, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar os headers de identidade", e);
        }
    }

    public static boolean verify(String chave, String payload, String assinatura) {
        if (assinatura == null || assinatura.isBlank()) {
            return false;
        }
        byte[] esperada = sign(chave, payload).getBytes(StandardCharsets.UTF_8);
        byte[] recebida = assinatura.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperada, recebida);
    }

    public static boolean dentroDaJanela(long timestamp, long agora, long toleranciaSegundos) {
        return Math.abs(agora - timestamp) <= toleranciaSegundos;
    }

    public static void exigirChaveSegura(String chave, boolean perfilDev) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY ausente. Defina a variavel de ambiente GATEWAY_SIGNING_KEY "
                    + "(>= " + TAMANHO_MINIMO + " bytes) para o gateway e para todos os servicos.");
        }
        if (chave.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY precisa ter ao menos " + TAMANHO_MINIMO + " bytes.");
        }
        if (CHAVE_DEV.equals(chave) && !perfilDev) {
            throw new IllegalStateException(
                    "GATEWAY_SIGNING_KEY inseguro. Defina a variavel de ambiente GATEWAY_SIGNING_KEY, "
                    + "ou rode com o profile 'dev' para desenvolvimento local. "
                    + "O servico recusa iniciar com a chave default fora de dev.");
        }
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
