package app.lexo.adapter.in.web;

import app.lexo.application.port.in.GetPortalQuery;
import app.lexo.application.port.in.GeneratePortalLinkCommand;
import app.lexo.application.port.in.GeneratePortalLinkUseCase;
import app.lexo.application.port.in.PortalLinkResponse;
import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.in.RevokePortalLinkCommand;
import app.lexo.application.port.in.RevokePortalLinkUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.ClientId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PortalController {

    private final GeneratePortalLinkUseCase generateLink;
    private final RevokePortalLinkUseCase revokeLink;
    private final GetPortalQuery getPortal;

    PortalController(GeneratePortalLinkUseCase generateLink, RevokePortalLinkUseCase revokeLink,
                     GetPortalQuery getPortal) {
        this.generateLink = generateLink;
        this.revokeLink = revokeLink;
        this.getPortal = getPortal;
    }

    @PostMapping("/api/clientes/{id}/portal")
    PortalLinkResponse generate(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return generateLink.generate(new GeneratePortalLinkCommand(Identity.organization(me), new ClientId(id)));
    }

    @DeleteMapping("/api/clientes/{id}/portal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        revokeLink.revoke(new RevokePortalLinkCommand(Identity.organization(me), new ClientId(id)));
    }

    @GetMapping("/api/portal/{token}")
    PortalResponse portal(@PathVariable String token) {
        return getPortal.get(token);
    }
}
