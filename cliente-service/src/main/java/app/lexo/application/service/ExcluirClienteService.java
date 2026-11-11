package app.lexo.application.service;

import app.lexo.application.port.in.ExcluirClienteCommand;
import app.lexo.application.port.in.ExcluirClienteUseCase;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.EventoDeCliente;
import app.lexo.application.port.out.ExcluirClientePort;
import app.lexo.application.port.out.PublicarEventoPort;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ExcluirClienteService implements ExcluirClienteUseCase {

    private final CarregarClientePort carregarClientePort;
    private final ExcluirClientePort excluirClientePort;
    private final PublicarEventoPort publicarEventoPort;

    ExcluirClienteService(CarregarClientePort carregarClientePort, ExcluirClientePort excluirClientePort,
                          PublicarEventoPort publicarEventoPort) {
        this.carregarClientePort = carregarClientePort;
        this.excluirClientePort = excluirClientePort;
        this.publicarEventoPort = publicarEventoPort;
    }

    @Override
    public void excluir(ExcluirClienteCommand command) {
        Cliente cliente = carregarClientePort.porId(command.escritorioId(), command.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
        excluirClientePort.excluir(cliente);
        publicarEventoPort.publicar(EventoDeCliente.excluido(cliente, command.autor()));
    }
}
