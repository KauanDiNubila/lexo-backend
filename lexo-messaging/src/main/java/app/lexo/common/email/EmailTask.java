package app.lexo.common.email;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record EmailTask(
        @JsonProperty("tipo") String type,
        @JsonProperty("destinatario") String recipient,
        @JsonProperty("assunto") String subject,
        @JsonProperty("corpo") String body
) implements Serializable {
}
