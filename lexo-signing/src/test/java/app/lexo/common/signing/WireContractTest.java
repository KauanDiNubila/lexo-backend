package app.lexo.common.signing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Signature wire contract between the gateway and the services")
class WireContractTest {

    private static final String KEY = "chave-de-teste-com-no-minimo-32-bytes-aqui";

    @Test
    @DisplayName("the headers keep their names")
    void headerNames() {
        assertEquals("X-Gateway-Ts", GatewaySignature.H_TIMESTAMP);
        assertEquals("X-Gateway-Sig", GatewaySignature.H_SIGNATURE);
    }

    @Test
    @DisplayName("the payload joins the six fields with a pipe, empty text for a missing one")
    void payloadFormat() {
        assertEquals("user-1|org-1|ADMIN|Ana|ana@x.com|1700000000",
                GatewaySignature.payload("user-1", "org-1", "ADMIN", "Ana", "ana@x.com", 1_700_000_000L));
        assertEquals("user-1|||||5", GatewaySignature.payload("user-1", null, null, null, null, 5));
    }

    @Test
    @DisplayName("the signature of a known payload is the one the other services compute")
    void knownSignature() {
        String payload = GatewaySignature.payload("user-1", "org-1", "ADMIN", "Ana", "ana@x.com", 1_700_000_000L);

        assertEquals("_Zbv2D2b7-EpRfIXr-xAKi_WbCM0vwJb22Qmmp8m7eM", GatewaySignature.sign(KEY, payload));
        assertTrue(GatewaySignature.verify(KEY, payload, "_Zbv2D2b7-EpRfIXr-xAKi_WbCM0vwJb22Qmmp8m7eM"));
    }

    @Test
    @DisplayName("the session cutoff key keeps its prefix")
    void cutoffKey() {
        assertEquals("session-cutoff:u-1", SessionCutoff.key("u-1"));
    }
}
