package app.lexo.application.service;

import app.lexo.application.ConfiguracaoDoPortal;
import app.lexo.application.port.in.GerarLinkDoPortalCommand;
import app.lexo.application.port.in.GerarLinkDoPortalUseCase;
import app.lexo.application.port.in.LinkDoPortalResponse;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteNaoEncontradoException;
import app.lexo.domain.LinkDoPortal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
class GerarLinkDoPortalService implements GerarLinkDoPortalUseCase {

    private final CarregarClientePort carregarClientePort;
    private final SalvarClientePort salvarClientePort;
    private final Clock clock;
    private final ConfiguracaoDoPortal configuracao;

    GerarLinkDoPortalService(CarregarClientePort carregarClientePort, SalvarClientePort salvarClientePort,
                             Clock clock, ConfiguracaoDoPortal configuracao) {
        this.carregarClientePort = carregarClientePort;
        this.salvarClientePort = salvarClientePort;
        this.clock = clock;
        this.configuracao = configuracao;
    }

    @Override
    public LinkDoPortalResponse gerar(GerarLinkDoPortalCommand command) {
        Cliente cliente = carregarClientePort.porId(command.escritorioId(), command.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
        LinkDoPortal link = cliente.gerarLinkDoPortal(novoToken(), clock.instant(), configuracao.validadeEmDias());
        salvarClientePort.salvar(cliente);
        return new LinkDoPortalResponse(link.token());
    }

    private String novoToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
