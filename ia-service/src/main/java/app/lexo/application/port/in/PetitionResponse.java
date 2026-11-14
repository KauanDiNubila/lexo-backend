package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PetitionResponse(
        @JsonProperty("texto") String text,
        @JsonProperty("fonte") String source
) {
}
