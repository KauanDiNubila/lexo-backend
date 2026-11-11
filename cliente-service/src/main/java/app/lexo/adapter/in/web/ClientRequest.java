package app.lexo.adapter.in.web;

import app.lexo.domain.TaxId;
import app.lexo.domain.ClientData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ClientRequest(
        @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
        String document,
        String email,
        String phone,
        String notes
) {

    ClientData toData() {
        return new ClientData(name, TaxId.optional(document), email, phone, notes);
    }
}
