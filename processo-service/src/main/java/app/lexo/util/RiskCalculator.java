package app.lexo.util;

import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.domain.enums.DeadlineType;

import java.time.Instant;

public final class RiskCalculator {

    private RiskCalculator() {
    }

    public enum RiskLevel {
        CRITICO("Crítico"),
        URGENTE("Urgente"),
        ALTO("Alto"),
        MEDIO("Médio"),
        BAIXO("Baixo");

        private final String label;

        RiskLevel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static RiskLevel getRiskLevel(Instant date, DeadlineType type, DeadlineStatus status) {
        if (status != DeadlineStatus.PENDENTE) return null;

        double daysRemaining =
                (date.toEpochMilli() - System.currentTimeMillis()) / (1000.0 * 60 * 60 * 24);

        double factor = type == DeadlineType.PRAZO ? 1.5
                : type == DeadlineType.AUDIENCIA ? 1.2
                : 1.0;
        double effective = daysRemaining / factor;

        if (effective <= 1) return RiskLevel.CRITICO;
        if (effective <= 3) return RiskLevel.URGENTE;
        if (effective <= 7) return RiskLevel.ALTO;
        if (effective <= 15) return RiskLevel.MEDIO;
        return RiskLevel.BAIXO;
    }
}
