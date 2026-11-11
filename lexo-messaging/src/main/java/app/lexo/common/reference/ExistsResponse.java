package app.lexo.common.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExistsResponse(@JsonProperty("existe") boolean exists) {
}
