package app.lexo.application.port.in;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PortalResponse(
        @JsonProperty("cliente") String client,
        @JsonProperty("processos") List<ProcessoPortal> cases,
        @JsonProperty("honorarios") List<HonorarioPortal> fees,
        @JsonProperty("resumo") PortalSummary summary
) {
}
