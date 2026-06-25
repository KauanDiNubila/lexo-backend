package app.lexo.controller;

import app.lexo.dto.TeamDtos.InviteRequest;
import app.lexo.dto.TeamDtos.InviteResponse;
import app.lexo.dto.TeamDtos.UpdateRoleRequest;
import app.lexo.dto.TeamDtos.UserResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.EquipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestao de equipe — restrita a ADMIN. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class EquipeController {

    private final EquipeService service;

    public EquipeController(EquipeService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> listarUsuarios(@AuthenticationPrincipal AuthUser me) {
        return service.listarUsuarios(me);
    }

    @GetMapping("/convites")
    public List<InviteResponse> listInvites(@AuthenticationPrincipal AuthUser me) {
        return service.listarConvitesPendentes(me);
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse invite(@AuthenticationPrincipal AuthUser me,
                                 @Valid @RequestBody InviteRequest req) {
        return service.convidar(me, req);
    }

    @DeleteMapping("/convites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogarConvite(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.revogarConvite(me, id);
    }

    @PatchMapping("/papel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarPapel(@AuthenticationPrincipal AuthUser me,
                           @Valid @RequestBody UpdateRoleRequest req) {
        service.alterarPapel(me, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerUsuario(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.removerUsuario(me, id);
    }
}
