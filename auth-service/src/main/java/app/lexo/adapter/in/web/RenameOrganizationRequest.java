package app.lexo.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RenameOrganizationRequest(
        @NotBlank @Size(min = 2, message = "Nome do escritório muito curto") String name
) {
}
