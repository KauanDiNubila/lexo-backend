package app.lexo.common.signing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Assinatura dos headers do gateway")
class GatewaySignatureTest {

    private static final String CHAVE = "chave-de-teste-com-no-minimo-32-bytes-aqui";
    private static final String OUTRA_CHAVE = "outra-chave-de-teste-com-no-minimo-32-bytes";

    private String payloadPadrao(long ts) {
        return GatewaySignature.payload("user-1", "org-1", "ADMIN", "Ana", "ana@x.com", ts);
    }

    @Test
    @DisplayName("assinatura gerada e aceita pela verificacao")
    void assinaEVerifica() {
        String payload = payloadPadrao(1_700_000_000L);
        String assinatura = GatewaySignature.sign(CHAVE, payload);

        assertTrue(GatewaySignature.verify(CHAVE, payload, assinatura));
    }

    @Test
    @DisplayName("mesma entrada produz sempre a mesma assinatura")
    void deterministica() {
        String payload = payloadPadrao(1_700_000_000L);

        assertEquals(GatewaySignature.sign(CHAVE, payload), GatewaySignature.sign(CHAVE, payload));
    }

    @Test
    @DisplayName("assinatura de outra chave e recusada")
    void chaveErradaFalha() {
        String payload = payloadPadrao(1_700_000_000L);
        String assinatura = GatewaySignature.sign(OUTRA_CHAVE, payload);

        assertFalse(GatewaySignature.verify(CHAVE, payload, assinatura));
    }

    @Test
    @DisplayName("trocar o papel invalida a assinatura")
    void payloadAdulteradoFalha() {
        String assinatura = GatewaySignature.sign(CHAVE, payloadPadrao(1_700_000_000L));
        String adulterado = GatewaySignature.payload(
                "user-1", "org-1", "SECRETARIA", "Ana", "ana@x.com", 1_700_000_000L);

        assertFalse(GatewaySignature.verify(CHAVE, adulterado, assinatura));
    }

    @Test
    @DisplayName("assinatura ausente ou vazia e recusada")
    void assinaturaAusenteFalha() {
        String payload = payloadPadrao(1_700_000_000L);

        assertFalse(GatewaySignature.verify(CHAVE, payload, null));
        assertFalse(GatewaySignature.verify(CHAVE, payload, "  "));
    }

    @Test
    @DisplayName("janela de tempo aceita o recente e recusa o antigo")
    void janelaDeTempo() {
        long agora = 1_700_000_000L;

        assertTrue(GatewaySignature.dentroDaJanela(agora - 30, agora, 60));
        assertTrue(GatewaySignature.dentroDaJanela(agora + 5, agora, 60));
        assertFalse(GatewaySignature.dentroDaJanela(agora - 120, agora, 60));
    }

    @Test
    @DisplayName("chave default so e aceita no profile dev")
    void chaveDefaultForaDeDevFalha() {
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.exigirChaveSegura(GatewaySignature.CHAVE_DEV, false));
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.exigirChaveSegura("", true));
        assertThrows(IllegalStateException.class,
                () -> GatewaySignature.exigirChaveSegura("curta-demais", true));

        GatewaySignature.exigirChaveSegura(GatewaySignature.CHAVE_DEV, true);
        GatewaySignature.exigirChaveSegura(CHAVE, false);
    }
}
