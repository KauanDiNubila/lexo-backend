package app.lexo.common.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PortalDtos {

    private PortalDtos() {
    }

    public record PrazoPortal(String title, String type, String status, Instant date) {
    }

    public record AndamentoPortal(String title, String description, Instant date) {
    }

    public record ProcessoPortal(
            String number,
            String area,
            String status,
            Instant createdAt,
            List<PrazoPortal> prazos,
            List<AndamentoPortal> andamentos
    ) {
    }

    public record HonorarioPortal(
            String description,
            BigDecimal amount,
            String status,
            Instant dueDate
    ) {
    }
}
