package app.lexo.dto;

import app.lexo.domain.Client;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ClientDtos {

    private ClientDtos() {
    }

    public record ClientRequest(
            @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
            String document,
            String email,
            String phone,
            String notes
    ) {
    }

    public record ClientResponse(
            String id,
            String name,
            String document,
            String email,
            String phone,
            String notes,
            Instant createdAt
    ) {
        public static ClientResponse from(Client c) {
            return new ClientResponse(
                    c.getId(), c.getName(), c.getDocument(), c.getEmail(),
                    c.getPhone(), c.getNotes(), c.getCreatedAt());
        }
    }
}
