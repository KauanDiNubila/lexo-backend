package app.lexo.adapter.in.web;

import app.lexo.domain.CaseContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

record CaseContextRequest(
        @Size(max = 60) @JsonProperty("numero") String number,
        @Size(max = 80) @JsonProperty("area") String area,
        @Size(max = 40) @JsonProperty("status") String status,
        @Size(max = 200) @JsonProperty("cliente") String client
) {

    CaseContext toDomain() {
        return new CaseContext(number, area, status, client);
    }
}
