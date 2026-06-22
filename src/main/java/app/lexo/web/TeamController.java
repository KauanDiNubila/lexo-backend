package app.lexo.web;

import app.lexo.dto.TeamDtos.InviteRequest;
import app.lexo.dto.TeamDtos.InviteResponse;
import app.lexo.dto.TeamDtos.UpdateRoleRequest;
import app.lexo.dto.TeamDtos.UserResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.TeamService;
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
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> listUsers(@AuthenticationPrincipal AuthUser me) {
        return service.listUsers(me);
    }

    @GetMapping("/convites")
    public List<InviteResponse> listInvites(@AuthenticationPrincipal AuthUser me) {
        return service.listPendingInvites(me);
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse invite(@AuthenticationPrincipal AuthUser me,
                                 @Valid @RequestBody InviteRequest req) {
        return service.invite(me, req);
    }

    @DeleteMapping("/convites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.revokeInvite(me, id);
    }

    @PatchMapping("/papel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRole(@AuthenticationPrincipal AuthUser me,
                           @Valid @RequestBody UpdateRoleRequest req) {
        service.updateRole(me, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUser(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        service.removeUser(me, id);
    }
}
