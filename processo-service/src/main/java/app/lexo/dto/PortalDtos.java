package app.lexo.dto;

import java.time.Instant;
import java.util.List;

public class PortalDtos {

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
}
