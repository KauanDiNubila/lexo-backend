package app.lexo.application.service;

import app.lexo.application.port.in.AtualizarClienteCommand;
import app.lexo.application.port.in.AtualizarClienteUseCase;
import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class AtualizarClienteService implements AtualizarClienteUseCase {

    private final CarregarClientePort carregarClientePort;
    private final SalvarClientePort salvarClientePort;
    private final Clock clock;

    AtualizarClienteService(CarregarClientePort carregarClientePort, SalvarClientePort salvarClientePort,
                            Clock clock) {
        this.carregarClientePort = carregarClientePort;
        this.salvarClientePort = salvarClientePort;
        this.clock = clock;
    }

    @Override
    public ClienteResponse atualizar(AtualizarClienteCommand command) {
        Cliente cliente = carregarClientePort.porId(command.escritorioId(), command.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
        cliente.atualizar(command.dados());
        return ClienteResponse.de(salvarClientePort.salvar(cliente), clock.instant());
    }
}
