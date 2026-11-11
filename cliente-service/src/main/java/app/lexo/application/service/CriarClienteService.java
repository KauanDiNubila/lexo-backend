package app.lexo.application.service;

import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.in.CriarClienteCommand;
import app.lexo.application.port.in.CriarClienteUseCase;
import app.lexo.application.port.out.EventoDeCliente;
import app.lexo.application.port.out.PublicarEventoPort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.domain.Cliente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class CriarClienteService implements CriarClienteUseCase {

    private final SalvarClientePort salvarClientePort;
    private final PublicarEventoPort publicarEventoPort;
    private final Clock clock;

    CriarClienteService(SalvarClientePort salvarClientePort, PublicarEventoPort publicarEventoPort, Clock clock) {
        this.salvarClientePort = salvarClientePort;
        this.publicarEventoPort = publicarEventoPort;
        this.clock = clock;
    }

    @Override
    public ClienteResponse criar(CriarClienteCommand command) {
        Cliente salvo = salvarClientePort.salvar(Cliente.cadastrar(command.escritorioId(), command.dados()));
        publicarEventoPort.publicar(EventoDeCliente.criado(salvo, command.autor()));
        return ClienteResponse.de(salvo, clock.instant());
    }
}
