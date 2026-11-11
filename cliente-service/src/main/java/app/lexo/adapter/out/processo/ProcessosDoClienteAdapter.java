package app.lexo.adapter.out.processo;

import app.lexo.application.port.out.CarregarProcessosDoClientePort;
import app.lexo.client.ProcessoPortalClient;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ProcessosDoClienteAdapter implements CarregarProcessosDoClientePort {

    private final ProcessoPortalClient cliente;

    ProcessosDoClienteAdapter(ProcessoPortalClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public List<ProcessoPortal> carregar(EscritorioId escritorioId, ClienteId clienteId) {
        return cliente.processosDoCliente(clienteId.valor(), escritorioId.valor());
    }
}
