package app.lexo.application.port.in;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;

import java.util.List;

public record PortalResponse(
        String cliente,
        List<ProcessoPortal> processos,
        List<HonorarioPortal> honorarios,
        ResumoPortal resumo
) {
}
