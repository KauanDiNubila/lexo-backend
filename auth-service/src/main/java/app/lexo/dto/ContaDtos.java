package app.lexo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ContaDtos {

    public record OrganizacaoInfo(
            String id,
            String name,
            String plan,
            Instant trialEndsAt,
            int membros
    ) {
    }

    public record PerfilInfo(
            String id,
            String name,
            String email,
            String role,
            boolean totpEnabled
    ) {
    }

    public record ContaResponse(
            OrganizacaoInfo organizacao,
            PerfilInfo usuario
    ) {
    }

    public record AtualizarOrganizacaoRequest(
            @NotBlank @Size(min = 2, message = "Nome do escritório muito curto") String name
    ) {
    }

    public record AtualizarPerfilRequest(
            @NotBlank @Size(min = 2, message = "Nome muito curto") String name
    ) {
    }
}
