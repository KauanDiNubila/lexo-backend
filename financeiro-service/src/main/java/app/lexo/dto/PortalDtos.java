package app.lexo.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Honorario de um cliente para o portal (consumido pelo cliente-service via Feign). */
public class PortalDtos {

    public record HonorarioPortal(
            String description,
            BigDecimal amount,
            String status,
            Instant dueDate
    ) {
    }
}
