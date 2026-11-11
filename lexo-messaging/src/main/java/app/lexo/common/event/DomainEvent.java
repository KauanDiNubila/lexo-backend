package app.lexo.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

public record DomainEvent(
        @JsonProperty("tipo") String type,
        String organizationId,
        String entityType,
        String entityId,
        @JsonProperty("descricao") String description,
        String userId,
        String userName,
        @JsonProperty("ocorridoEm") Instant occurredAt
) implements Serializable {

    public boolean is(EventType expected) {
        return expected.code().equals(type);
    }

    public static DomainEvent of(String type, String organizationId, String entityType,
                                 String entityId, String description, String userId, String userName) {
        return new DomainEvent(type, organizationId, entityType, entityId, description,
                userId, userName, Instant.now());
    }
}
