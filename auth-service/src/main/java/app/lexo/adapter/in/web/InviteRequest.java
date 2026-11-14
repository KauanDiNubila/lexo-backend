package app.lexo.adapter.in.web;

import app.lexo.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record InviteRequest(
        @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
        @NotBlank @Email(message = "Email inválido") String email,
        @NotNull Role role
) {
}
