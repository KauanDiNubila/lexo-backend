package app.lexo.controller;

import app.lexo.dto.TeamDtos.AcceptInviteRequest;
import app.lexo.dto.TeamDtos.InviteInfo;
import app.lexo.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Aceite de convite por link — rotas publicas (sem login). */
@RestController
@RequestMapping("/api/convites")
public class InviteController {

    private final TeamService service;

    public InviteController(TeamService service) {
        this.service = service;
    }

    @GetMapping("/info/{token}")
    public InviteInfo info(@PathVariable String token) {
        return service.inviteInfo(token);
    }

    @PostMapping("/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody AcceptInviteRequest req) {
        service.acceptInvite(req);
    }
}
