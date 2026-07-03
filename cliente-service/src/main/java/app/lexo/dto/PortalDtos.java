package app.lexo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTOs do portal do cliente (visao publica read-only, agregada de varios servicos). */
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

    /** Resposta publica do portal, montada a partir do token. */
    public record PortalResponse(
            String cliente,
            List<ProcessoPortal> processos,
            List<HonorarioPortal> honorarios,
            ResumoPortal resumo
    ) {
    }

    /** Retorno ao gerar/rotacionar o link do portal (o advogado copia e envia ao cliente). */
    public record PortalLinkResponse(String token) {
    }
}
