package app.lexo.dto;

import app.lexo.domain.Client;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
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
            boolean portalAtivo,
            Instant portalExpiraEm,
            Instant createdAt
    ) implements Serializable {
        public static ClientResponse from(Client c) {
            return new ClientResponse(
                    c.getId(), c.getName(), c.getDocument(), c.getEmail(),
                    c.getPhone(), c.getNotes(), c.portalAtivo(),
                    c.getPortalTokenExpiresAt(), c.getCreatedAt());
        }
    }

    public record ClienteOpcao(String id, String name) implements Serializable {
        public static ClienteOpcao from(Client c) {
            return new ClienteOpcao(c.getId(), c.getName());
        }
    }

    public record ResumoClientes(long total) {
    }
}
