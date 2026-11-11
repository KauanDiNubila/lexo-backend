package app.lexo.application.port.out;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import java.util.List;

public interface CarregarHonorariosDoClientePort {

    List<HonorarioPortal> carregar(EscritorioId escritorioId, ClienteId clienteId);
}
