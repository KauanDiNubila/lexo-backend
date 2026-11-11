package app.lexo.application.service;

import app.lexo.application.port.in.BuscarClienteQuery;
import app.lexo.application.port.in.ClienteOpcao;
import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.in.ExisteClienteQuery;
import app.lexo.application.port.in.ListarClientesQuery;
import app.lexo.application.port.in.ListarOpcoesDeClienteQuery;
import app.lexo.application.port.in.Paginacao;
import app.lexo.application.port.in.ResumoClientes;
import app.lexo.application.port.in.ResumoDeClientesQuery;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.ContarClientesPort;
import app.lexo.application.port.out.ExisteClientePort;
import app.lexo.application.port.out.ListarClientesPort;
import app.lexo.application.port.out.ListarOpcoesDeClientePort;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.ClienteNaoEncontradoException;
import app.lexo.domain.EscritorioId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
class ClienteQueryService implements BuscarClienteQuery, ListarClientesQuery, ListarOpcoesDeClienteQuery,
        ResumoDeClientesQuery, ExisteClienteQuery {

    private final CarregarClientePort carregarClientePort;
    private final ListarClientesPort listarClientesPort;
    private final ListarOpcoesDeClientePort listarOpcoesPort;
    private final ContarClientesPort contarClientesPort;
    private final ExisteClientePort existeClientePort;
    private final Clock clock;

    ClienteQueryService(CarregarClientePort carregarClientePort, ListarClientesPort listarClientesPort,
                        ListarOpcoesDeClientePort listarOpcoesPort, ContarClientesPort contarClientesPort,
                        ExisteClientePort existeClientePort, Clock clock) {
        this.carregarClientePort = carregarClientePort;
        this.listarClientesPort = listarClientesPort;
        this.listarOpcoesPort = listarOpcoesPort;
        this.contarClientesPort = contarClientesPort;
        this.existeClientePort = existeClientePort;
        this.clock = clock;
    }

    @Override
    public ClienteResponse buscar(EscritorioId escritorioId, ClienteId clienteId) {
        Cliente cliente = carregarClientePort.porId(escritorioId, clienteId)
                .orElseThrow(ClienteNaoEncontradoException::new);
        return ClienteResponse.de(cliente, clock.instant());
    }

    @Override
    public Pagina<ClienteResponse> listar(EscritorioId escritorioId, String busca, Paginacao paginacao) {
        Pagina<Cliente> pagina = listarClientesPort.listar(escritorioId, busca, paginacao);
        Instant agora = clock.instant();
        List<ClienteResponse> conteudo = pagina.content().stream().map(c -> ClienteResponse.de(c, agora)).toList();
        return new Pagina<>(conteudo, pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
    }

    @Override
    public List<ClienteOpcao> listarOpcoes(EscritorioId escritorioId) {
        return listarOpcoesPort.opcoes(escritorioId);
    }

    @Override
    public ResumoClientes resumir(EscritorioId escritorioId) {
        return new ResumoClientes(contarClientesPort.contar(escritorioId));
    }

    @Override
    public boolean existe(EscritorioId escritorioId, ClienteId clienteId) {
        return existeClientePort.existe(escritorioId, clienteId);
    }
}
