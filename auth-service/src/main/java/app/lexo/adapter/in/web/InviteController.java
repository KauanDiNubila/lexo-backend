package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AcceptInviteUseCase;
import app.lexo.application.port.in.GetInviteInfoQuery;
import app.lexo.application.port.in.InviteInfo;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convites")
class InviteController {

    private final GetInviteInfoQuery getInviteInfoQuery;
    private final AcceptInviteUseCase acceptInviteUseCase;

    InviteController(GetInviteInfoQuery getInviteInfoQuery, AcceptInviteUseCase acceptInviteUseCase) {
        this.getInviteInfoQuery = getInviteInfoQuery;
        this.acceptInviteUseCase = acceptInviteUseCase;
    }

    @GetMapping("/info/{token}")
    InviteInfo info(@PathVariable String token) {
        return getInviteInfoQuery.get(token);
    }

    @PostMapping("/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void accept(@Valid @RequestBody AcceptInviteRequest request) {
        acceptInviteUseCase.accept(request.toCommand());
    }
}
