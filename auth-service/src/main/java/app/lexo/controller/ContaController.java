package app.lexo.controller;

import app.lexo.dto.ContaDtos.AtualizarOrganizacaoRequest;
import app.lexo.dto.ContaDtos.AtualizarPerfilRequest;
import app.lexo.dto.ContaDtos.ContaResponse;
import app.lexo.dto.ContaDtos.OrganizacaoInfo;
import app.lexo.dto.ContaDtos.PerfilInfo;
import app.lexo.security.AuthUser;
import app.lexo.service.ContaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Area de conta: dados do escritorio (organizacao) e do perfil do usuario logado.
 * O nome do escritorio so pode ser alterado por ADMIN; o perfil, pelo proprio usuario.
 */
@RestController
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @GetMapping("/api/conta")
    public ContaResponse conta(@AuthenticationPrincipal AuthUser me) {
        return service.obterConta(me);
    }

    @PutMapping("/api/organizacao")
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizacaoInfo atualizarOrganizacao(@AuthenticationPrincipal AuthUser me,
                                                @Valid @RequestBody AtualizarOrganizacaoRequest req) {
        return service.atualizarOrganizacao(me, req.name());
    }

    @PutMapping("/api/perfil")
    public PerfilInfo atualizarPerfil(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody AtualizarPerfilRequest req) {
        return service.atualizarPerfil(me, req.name());
    }
}
