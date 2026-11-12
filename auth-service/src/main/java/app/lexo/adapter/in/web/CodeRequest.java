package app.lexo.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record CodeRequest(
        @NotBlank(message = "Código é obrigatório") String code
) {
}
