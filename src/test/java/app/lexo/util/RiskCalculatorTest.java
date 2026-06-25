package app.lexo.util;

import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.domain.enums.DeadlineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Calculo de risco de prazo")
class RiskCalculatorTest {

    private Instant emDias(long dias) {
        return Instant.now().plus(dias, ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("prazo nao pendente nao tem risco (null)")
    void naoPendenteSemRisco() {
        assertNull(RiskCalculator.getRiskLevel(emDias(0), DeadlineType.PRAZO, DeadlineStatus.CONCLUIDO));
        assertNull(RiskCalculator.getRiskLevel(emDias(0), DeadlineType.PRAZO, DeadlineStatus.PERDIDO));
    }

    @Test
    @DisplayName("vencimento muito proximo = CRITICO")
    void critico() {
        Instant em12h = Instant.now().plus(12, ChronoUnit.HOURS);
        assertEquals(RiskCalculator.RiskLevel.CRITICO,
                RiskCalculator.getRiskLevel(em12h, DeadlineType.OUTRO, DeadlineStatus.PENDENTE));
    }

    @Test
    @DisplayName("PRAZO pesa mais que OUTRO (fator 1.5)")
    void prazoPesaMais() {
        // 2 dias: como OUTRO daria ALTO; como PRAZO o vencimento efetivo chega antes -> URGENTE
        assertEquals(RiskCalculator.RiskLevel.URGENTE,
                RiskCalculator.getRiskLevel(emDias(2), DeadlineType.PRAZO, DeadlineStatus.PENDENTE));
    }

    @Test
    @DisplayName("escala por proximidade: ALTO, MEDIO e BAIXO")
    void escala() {
        assertEquals(RiskCalculator.RiskLevel.ALTO,
                RiskCalculator.getRiskLevel(emDias(5), DeadlineType.OUTRO, DeadlineStatus.PENDENTE));
        assertEquals(RiskCalculator.RiskLevel.MEDIO,
                RiskCalculator.getRiskLevel(emDias(10), DeadlineType.OUTRO, DeadlineStatus.PENDENTE));
        assertEquals(RiskCalculator.RiskLevel.BAIXO,
                RiskCalculator.getRiskLevel(emDias(100), DeadlineType.OUTRO, DeadlineStatus.PENDENTE));
    }
}
