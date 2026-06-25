package app.lexo.dto;

import app.lexo.domain.User;
import app.lexo.domain.UserInvite;
import app.lexo.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTOs de gestao de equipe (usuarios + convites). */
public final class TeamDtos {

    private TeamDtos() {
    }

    public record InviteRequest(
            @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
            @NotBlank @Email(message = "Email inválido") String email,
            @NotNull Role role
    ) {
    }

    public record UpdateRoleRequest(
            @NotBlank String userId,
            @NotNull Role role
    ) {
    }

    public record AcceptInviteRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
            @NotBlank String confirmPassword
    ) {
    }

    public record UserResponse(
            String id,
            String name,
            String email,
            Role role,
            boolean totpEnabled,
            Instant createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.isTotpEnabled(), u.getCreatedAt());
        }
    }

    public record InviteResponse(
            String id,
            String name,
            String email,
            Role role,
            Instant expiresAt,
            Instant createdAt
    ) {
        public static InviteResponse from(UserInvite i) {
            return new InviteResponse(
                    i.getId(), i.getName(), i.getEmail(), i.getRole(),
                    i.getExpiresAt(), i.getCreatedAt());
        }
    }

    /** Info publica do convite (tela de aceite, sem exigir login). */
    public record InviteInfo(
            String organizationName,
            String inviteeName,
            String email,
            boolean valid,
            String reason
    ) {
    }
}
