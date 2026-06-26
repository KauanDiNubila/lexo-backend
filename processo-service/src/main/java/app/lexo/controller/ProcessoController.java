package app.lexo.controller;

import app.lexo.dto.CaseDtos.CaseRequest;
import app.lexo.dto.CaseDtos.CaseResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.ProcessoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
public class ProcessoController {

    private final ProcessoService service;

    public ProcessoController(ProcessoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CaseResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.listar(me);
    }

    @GetMapping("/{id}")
    public CaseResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.buscar(me, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse create(@AuthenticationPrincipal AuthUser me,
                               @Valid @RequestBody CaseRequest req) {
        return service.criar(me, req);
    }

    @PutMapping("/{id}")
    public CaseResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                               @Valid @RequestBody CaseRequest req) {
        return service.atualizar(me, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.excluir(me, id);
    }
}
