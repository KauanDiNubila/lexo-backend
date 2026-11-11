package app.lexo.adapter.in.web;

import app.lexo.domain.CpfCnpj;
import app.lexo.domain.DadosDoCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ClienteRequest(
        @NotBlank @Size(min = 2, message = "Nome muito curto") String name,
        String document,
        String email,
        String phone,
        String notes
) {

    DadosDoCliente paraDados() {
        return new DadosDoCliente(name, CpfCnpj.opcional(document), email, phone, notes);
    }
}
