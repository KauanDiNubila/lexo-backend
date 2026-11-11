package app.lexo.application.port.in;

import app.lexo.domain.Client;
import app.lexo.domain.ClientData;
import app.lexo.domain.PortalLink;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

public record ClientResponse(
        String id,
        String name,
        String document,
        String email,
        String phone,
        String notes,
        @JsonProperty("portalAtivo") boolean portalActive,
        @JsonProperty("portalExpiraEm") Instant portalExpiresAt,
        Instant createdAt
) implements Serializable {

    public static ClientResponse of(Client client, Instant now) {
        ClientData data = client.data();
        PortalLink link = client.portalLink();
        return new ClientResponse(
                client.id().value(),
                data.name(),
                data.taxId() == null ? null : data.taxId().text(),
                data.email(),
                data.phone(),
                data.notes(),
                client.portalActiveAt(now),
                link == null ? null : link.expiresAt(),
                client.createdAt());
    }
}
