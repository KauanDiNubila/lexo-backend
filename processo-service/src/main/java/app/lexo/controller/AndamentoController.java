package app.lexo.controller;

import app.lexo.dto.AndamentoDtos.AndamentoRequest;
import app.lexo.dto.AndamentoDtos.AndamentoResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.AndamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processos/{caseId}/andamentos")
public class AndamentoController {

    private final AndamentoService service;

    public AndamentoController(AndamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AndamentoResponse> list(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId) {
        return service.listar(me, caseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AndamentoResponse create(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId,
                                    @Valid @RequestBody AndamentoRequest req) {
        return service.criar(me, caseId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String caseId, @PathVariable String id) {
        service.excluir(me, caseId, id);
    }
}
