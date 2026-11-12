package app.lexo.adapter.in.web;

import app.lexo.common.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record ChangeRoleRequest(
        @NotBlank String userId,
        @NotNull Role role
) {
}
