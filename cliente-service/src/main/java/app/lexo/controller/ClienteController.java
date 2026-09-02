package app.lexo.controller;

import app.lexo.common.security.AuthUser;
import app.lexo.common.web.Pagina;
import app.lexo.dto.ClientDtos.ClienteOpcao;
import app.lexo.dto.ClientDtos.ClientRequest;
import app.lexo.dto.ClientDtos.ClientResponse;
import app.lexo.dto.ClientDtos.ResumoClientes;
import app.lexo.service.ClienteService;
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
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<ClientResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(me, q, pageable);
    }

    @GetMapping("/opcoes")
    public List<ClienteOpcao> opcoes(@AuthenticationPrincipal AuthUser me) {
        return service.opcoes(me);
    }

    @GetMapping("/resumo")
    public ResumoClientes resumo(@AuthenticationPrincipal AuthUser me) {
        return service.resumo(me);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.buscar(me, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@AuthenticationPrincipal AuthUser me,
                                 @Valid @RequestBody ClientRequest req) {
        return service.criar(me, req);
    }

    @PutMapping("/{id}")
    public ClientResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                 @Valid @RequestBody ClientRequest req) {
        return service.atualizar(me, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADVOGADO')")
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.excluir(me, id);
    }
}
