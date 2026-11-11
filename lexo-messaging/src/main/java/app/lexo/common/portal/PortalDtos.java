package app.lexo.common.portal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PortalDtos {

    private PortalDtos() {
    }

    public record PortalDeadline(String title, String type, String status, Instant date) {
    }

    public record PortalUpdate(String title, String description, Instant date) {
    }

    public record PortalCase(
            String number,
            String area,
            String status,
            Instant createdAt,
            @JsonProperty("prazos") List<PortalDeadline> deadlines,
            @JsonProperty("andamentos") List<PortalUpdate> updates
    ) {
    }

    public record PortalFee(
            String description,
            BigDecimal amount,
            String status,
            Instant dueDate
    ) {
    }
}
