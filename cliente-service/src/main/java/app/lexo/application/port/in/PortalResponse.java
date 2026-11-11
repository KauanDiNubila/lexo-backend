package app.lexo.application.port.in;

import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.common.portal.PortalDtos.PortalCase;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PortalResponse(
        @JsonProperty("cliente") String client,
        @JsonProperty("processos") List<PortalCase> cases,
        @JsonProperty("honorarios") List<PortalFee> fees,
        @JsonProperty("resumo") PortalSummary summary
) {
}
