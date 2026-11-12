package app.lexo.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountResponse(@JsonProperty("organizacao") OrganizationInfo organization,
                              @JsonProperty("usuario") ProfileInfo user) {
}
