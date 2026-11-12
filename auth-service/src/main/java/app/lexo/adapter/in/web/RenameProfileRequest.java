package app.lexo.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RenameProfileRequest(
        @NotBlank @Size(min = 2, message = "Nome muito curto") String name
) {
}
