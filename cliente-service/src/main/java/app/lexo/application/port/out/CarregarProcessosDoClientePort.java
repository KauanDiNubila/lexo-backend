package app.lexo.application.port.out;

import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import java.util.List;

public interface CarregarProcessosDoClientePort {

    List<ProcessoPortal> carregar(EscritorioId escritorioId, ClienteId clienteId);
}
