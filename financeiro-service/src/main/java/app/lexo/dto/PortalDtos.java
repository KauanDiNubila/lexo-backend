package app.lexo.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PortalDtos {

    public record HonorarioPortal(
            String description,
            BigDecimal amount,
            String status,
            Instant dueDate
    ) {
    }
}
