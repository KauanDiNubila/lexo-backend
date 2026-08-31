package app.lexo.dto;

import jakarta.validation.constraints.Size;

public record ProcessoContexto(
        @Size(max = 60) String numero,
        @Size(max = 80) String area,
        @Size(max = 40) String status,
        @Size(max = 200) String cliente
) {
}
