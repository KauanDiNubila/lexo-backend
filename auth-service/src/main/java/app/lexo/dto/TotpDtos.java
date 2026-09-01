package app.lexo.dto;

import jakarta.validation.constraints.NotBlank;

public final class TotpDtos {

    private TotpDtos() {
    }

    public record InitiateResponse(
            String secret,
            String otpauthUri
    ) {
    }

    public record CodeRequest(
            @NotBlank(message = "Código é obrigatório") String code
    ) {
    }
}
