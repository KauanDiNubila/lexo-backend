package app.lexo.adapter.in.web;

import app.lexo.application.port.in.SummarizeCaseCommand;
import app.lexo.domain.CaseContext;
import app.lexo.domain.OrganizationId;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

record SummarizeRequest(
        @Size(max = 60) @JsonProperty("numero") String number,
        @Size(max = 80) @JsonProperty("area") String area,
        @Size(max = 40) @JsonProperty("status") String status,
        @Size(max = 200) @JsonProperty("cliente") String client,
        @Size(max = 100, message = "Prazos demais para resumir")
        @JsonProperty("prazos") List<@Valid DeadlineRequest> deadlines
) {

    SummarizeCaseCommand toCommand(OrganizationId organizationId) {
        return new SummarizeCaseCommand(organizationId, new CaseContext(number, area, status, client),
                deadlines == null ? null : deadlines.stream().map(DeadlineRequest::toDomain).toList());
    }
}
