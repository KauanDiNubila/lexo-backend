package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ChangeUserRoleCommand;
import app.lexo.application.port.in.ChangeUserRoleUseCase;
import app.lexo.application.port.in.InviteResponse;
import app.lexo.application.port.in.InviteUserCommand;
import app.lexo.application.port.in.InviteUserUseCase;
import app.lexo.application.port.in.ListPendingInvitesQuery;
import app.lexo.application.port.in.ListUsersQuery;
import app.lexo.application.port.in.RemoveUserCommand;
import app.lexo.application.port.in.RemoveUserUseCase;
import app.lexo.application.port.in.RevokeInviteCommand;
import app.lexo.application.port.in.RevokeInviteUseCase;
import app.lexo.application.port.in.UserResponse;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.InviteId;
import app.lexo.domain.UserId;
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

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
class TeamController {

    private final ListUsersQuery listUsersQuery;
    private final ListPendingInvitesQuery listPendingInvitesQuery;
    private final InviteUserUseCase inviteUserUseCase;
    private final RevokeInviteUseCase revokeInviteUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final RemoveUserUseCase removeUserUseCase;

    TeamController(ListUsersQuery listUsersQuery, ListPendingInvitesQuery listPendingInvitesQuery,
                   InviteUserUseCase inviteUserUseCase, RevokeInviteUseCase revokeInviteUseCase,
                   ChangeUserRoleUseCase changeUserRoleUseCase, RemoveUserUseCase removeUserUseCase) {
        this.listUsersQuery = listUsersQuery;
        this.listPendingInvitesQuery = listPendingInvitesQuery;
        this.inviteUserUseCase = inviteUserUseCase;
        this.revokeInviteUseCase = revokeInviteUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.removeUserUseCase = removeUserUseCase;
    }

    @GetMapping
    List<UserResponse> users(@AuthenticationPrincipal AuthUser me) {
        return listUsersQuery.list(Identity.organization(me));
    }

    @GetMapping("/convites")
    List<InviteResponse> pendingInvites(@AuthenticationPrincipal AuthUser me) {
        return listPendingInvitesQuery.list(Identity.organization(me));
    }

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    InviteResponse invite(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody InviteRequest request) {
        return inviteUserUseCase.invite(new InviteUserCommand(Identity.organization(me), Identity.actor(me),
                request.name(), new EmailAddress(request.email()), request.role()));
    }

    @DeleteMapping("/convites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeInvite(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        revokeInviteUseCase.revoke(
                new RevokeInviteCommand(Identity.organization(me), Identity.actor(me), new InviteId(id)));
    }

    @PatchMapping("/papel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeRole(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangeRoleRequest request) {
        changeUserRoleUseCase.changeRole(new ChangeUserRoleCommand(Identity.organization(me), Identity.actor(me),
                new UserId(request.userId()), request.role()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeUser(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        removeUserUseCase.remove(
                new RemoveUserCommand(Identity.organization(me), Identity.actor(me), new UserId(id)));
    }
}
