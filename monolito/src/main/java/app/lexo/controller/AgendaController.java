package app.lexo.controller;

import app.lexo.dto.DeadlineDtos.DeadlineRequest;
import app.lexo.dto.DeadlineDtos.DeadlineResponse;
import app.lexo.dto.DeadlineDtos.StatusRequest;
import app.lexo.security.AuthUser;
import app.lexo.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Agenda (prazos/compromissos). */
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    private final AgendaService service;

    public AgendaController(AgendaService service) {
        this.service = service;
    }

    @GetMapping
    public List<DeadlineResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.listar(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeadlineResponse create(@AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody DeadlineRequest req) {
        return service.criar(me, req);
    }

    @PutMapping("/{id}")
    public DeadlineResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                   @Valid @RequestBody DeadlineRequest req) {
        return service.atualizar(me, id, req);
    }

    @PatchMapping("/{id}/status")
    public DeadlineResponse alternarStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                         @RequestBody StatusRequest req) {
        return service.alternarStatus(me, id, req.completed());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.excluir(me, id);
    }
}
