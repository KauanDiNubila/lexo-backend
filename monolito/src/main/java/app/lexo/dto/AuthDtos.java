package app.lexo.dto;

import app.lexo.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs de autenticacao/registro. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, message = "Nome do escritório muito curto") String organizationName,
            @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
            @NotBlank @Email(message = "Email inválido") String email,
            @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
            @NotBlank String confirmPassword
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email(message = "Email inválido") String email,
            @NotBlank String password,
            String totpCode
    ) {
    }

    public record UserInfo(
            String id,
            String name,
            String email,
            Role role,
            String organizationId
    ) {
    }

    public record AuthResponse(
            String token,
            UserInfo user
    ) {
    }
}
