package app.lexo.common.signing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Gateway header signature")
class GatewaySignatureTest {

    private static final String KEY = "chave-de-teste-com-no-minimo-32-bytes-aqui";
    private static final String OTHER_KEY = "outra-chave-de-teste-com-no-minimo-32-bytes";

    private String defaultPayload(long ts) {
        return GatewaySignature.payload("user-1", "org-1", "ADMIN", "Ana", "ana@x.com", ts);
    }

    @Test
    @DisplayName("a signature is accepted by the verification")
    void signsAndVerifies() {
        String payload = defaultPayload(1_700_000_000L);
        String signature = GatewaySignature.sign(KEY, payload);

        assertTrue(GatewaySignature.verify(KEY, payload, signature));
    }

    @Test
    @DisplayName("the same input always gives the same signature")
    void isDeterministic() {
        String payload = defaultPayload(1_700_000_000L);

        assertEquals(GatewaySignature.sign(KEY, payload), GatewaySignature.sign(KEY, payload));
    }

    @Test
    @DisplayName("a signature made with another key is refused")
    void wrongKeyFails() {
        String payload = defaultPayload(1_700_000_000L);
        String signature = GatewaySignature.sign(OTHER_KEY, payload);

        assertFalse(GatewaySignature.verify(KEY, payload, signature));
    }

    @Test
    @DisplayName("changing the role invalidates the signature")
    void tamperedPayloadFails() {
        String signature = GatewaySignature.sign(KEY, defaultPayload(1_700_000_000L));
        String tampered = GatewaySignature.payload(
                "user-1", "org-1", "SECRETARIA", "Ana", "ana@x.com", 1_700_000_000L);

        assertFalse(GatewaySignature.verify(KEY, tampered, signature));
    }

    @Test
    @DisplayName("a missing or blank signature is refused")
    void missingSignatureFails() {
        String payload = defaultPayload(1_700_000_000L);

        assertFalse(GatewaySignature.verify(KEY, payload, null));
        assertFalse(GatewaySignature.verify(KEY, payload, "  "));
    }

    @Test
    @DisplayName("the time window accepts the recent and refuses the old")
    void timeWindow() {
        long now = 1_700_000_000L;

        assertTrue(GatewaySignature.withinWindow(now - 30, now, 60));
        assertTrue(GatewaySignature.withinWindow(now + 5, now, 60));
        assertFalse(GatewaySignature.withinWindow(now - 120, now, 60));
    }

    @Test
    @DisplayName("the default key is only accepted in the dev profile")
    void defaultKeyOutsideDevFails() {
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.requireSecureKey(GatewaySignature.DEV_KEY, false));
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.requireSecureKey("", true));
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.requireSecureKey("curta-demais", true));

        GatewaySignature.requireSecureKey(GatewaySignature.DEV_KEY, true);
        GatewaySignature.requireSecureKey(KEY, false);
    }
}
