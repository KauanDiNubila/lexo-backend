package app.lexo.application.service;

import app.lexo.application.port.in.RevogarLinkDoPortalCommand;
import app.lexo.application.port.in.RevogarLinkDoPortalUseCase;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RevogarLinkDoPortalService implements RevogarLinkDoPortalUseCase {

    private final CarregarClientePort carregarClientePort;
    private final SalvarClientePort salvarClientePort;

    RevogarLinkDoPortalService(CarregarClientePort carregarClientePort, SalvarClientePort salvarClientePort) {
        this.carregarClientePort = carregarClientePort;
        this.salvarClientePort = salvarClientePort;
    }

    @Override
    public void revogar(RevogarLinkDoPortalCommand command) {
        Cliente cliente = carregarClientePort.porId(command.escritorioId(), command.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
        cliente.revogarLinkDoPortal();
        salvarClientePort.salvar(cliente);
    }
}
