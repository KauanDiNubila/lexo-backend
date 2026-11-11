package app.lexo.adapter.out.financeiro;

import app.lexo.application.port.out.CarregarHonorariosDoClientePort;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class HonorariosDoClienteAdapter implements CarregarHonorariosDoClientePort {

    private final FinanceiroPortalClient cliente;

    HonorariosDoClienteAdapter(FinanceiroPortalClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public List<HonorarioPortal> carregar(EscritorioId escritorioId, ClienteId clienteId) {
        return cliente.honorariosDoCliente(clienteId.valor(), escritorioId.valor());
    }
}
