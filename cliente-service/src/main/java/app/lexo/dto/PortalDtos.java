package app.lexo.dto;

import java.math.BigDecimal;
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

    public record HonorarioPortal(
            String description,
            BigDecimal amount,
            String status,
            Instant dueDate
    ) {
    }

    public record ResumoPortal(int totalProcessos, BigDecimal emAberto) {
    }

    public record PortalResponse(
            String cliente,
            List<ProcessoPortal> processos,
            List<HonorarioPortal> honorarios,
            ResumoPortal resumo
    ) {
    }

    public record PortalLinkResponse(String token) {
    }
}
