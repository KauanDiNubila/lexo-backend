package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AtualizarClienteCommand;
import app.lexo.application.port.in.AtualizarClienteUseCase;
import app.lexo.application.port.in.BuscarClienteQuery;
import app.lexo.application.port.in.ClienteOpcao;
import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.in.CriarClienteCommand;
import app.lexo.application.port.in.CriarClienteUseCase;
import app.lexo.application.port.in.ExcluirClienteCommand;
import app.lexo.application.port.in.ExcluirClienteUseCase;
import app.lexo.application.port.in.ListarClientesQuery;
import app.lexo.application.port.in.ListarOpcoesDeClienteQuery;
import app.lexo.application.port.in.ResumoClientes;
import app.lexo.application.port.in.ResumoDeClientesQuery;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.Pagina;
import app.lexo.domain.ClienteId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
class ClienteController {

    private final CriarClienteUseCase criarCliente;
    private final AtualizarClienteUseCase atualizarCliente;
    private final ExcluirClienteUseCase excluirCliente;
    private final BuscarClienteQuery buscarCliente;
    private final ListarClientesQuery listarClientes;
    private final ListarOpcoesDeClienteQuery listarOpcoes;
    private final ResumoDeClientesQuery resumoDeClientes;

    ClienteController(CriarClienteUseCase criarCliente, AtualizarClienteUseCase atualizarCliente,
                      ExcluirClienteUseCase excluirCliente, BuscarClienteQuery buscarCliente,
                      ListarClientesQuery listarClientes, ListarOpcoesDeClienteQuery listarOpcoes,
                      ResumoDeClientesQuery resumoDeClientes) {
        this.criarCliente = criarCliente;
        this.atualizarCliente = atualizarCliente;
        this.excluirCliente = excluirCliente;
        this.buscarCliente = buscarCliente;
        this.listarClientes = listarClientes;
        this.listarOpcoes = listarOpcoes;
        this.resumoDeClientes = resumoDeClientes;
    }

    @GetMapping
    Pagina<ClienteResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return listarClientes.listar(Identidade.escritorio(me), q, Identidade.paginacao(pageable));
    }

    @GetMapping("/opcoes")
    List<ClienteOpcao> opcoes(@AuthenticationPrincipal AuthUser me) {
        return listarOpcoes.listar(Identidade.escritorio(me));
    }

    @GetMapping("/resumo")
    ResumoClientes resumo(@AuthenticationPrincipal AuthUser me) {
        return resumoDeClientes.resumir(Identidade.escritorio(me));
    }

    @GetMapping("/{id}")
    ClienteResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return buscarCliente.buscar(Identidade.escritorio(me), new ClienteId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClienteResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ClienteRequest req) {
        return criarCliente.criar(new CriarClienteCommand(Identidade.escritorio(me), Identidade.autor(me), req.paraDados()));
    }

    @PutMapping("/{id}")
    ClienteResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                           @Valid @RequestBody ClienteRequest req) {
        return atualizarCliente.atualizar(
                new AtualizarClienteCommand(Identidade.escritorio(me), new ClienteId(id), req.paraDados()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADVOGADO')")
    void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        excluirCliente.excluir(new ExcluirClienteCommand(Identidade.escritorio(me), new ClienteId(id), Identidade.autor(me)));
    }
}
