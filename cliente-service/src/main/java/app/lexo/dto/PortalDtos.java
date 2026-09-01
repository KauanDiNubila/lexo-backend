package app.lexo.dto;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;

import java.math.BigDecimal;
import java.util.List;

public final class PortalDtos {

    private PortalDtos() {
    }

    public record ResumoPortal(int totalProcessos, BigDecimal emAberto) {
    }

    public record PortalResponse(
            String cliente,
            List<ProcessoPortal> processos,
            List<HonorarioPortal> honorarios,
            ResumoPortal resumo
    ) {
    }

    public record PortalLinkResponse(String token) {
    }
}
