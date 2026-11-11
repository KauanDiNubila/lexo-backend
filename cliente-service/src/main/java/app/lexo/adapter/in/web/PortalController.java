package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ConsultarPortalQuery;
import app.lexo.application.port.in.GerarLinkDoPortalCommand;
import app.lexo.application.port.in.GerarLinkDoPortalUseCase;
import app.lexo.application.port.in.LinkDoPortalResponse;
import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.in.RevogarLinkDoPortalCommand;
import app.lexo.application.port.in.RevogarLinkDoPortalUseCase;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.ClienteId;
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

    private final GerarLinkDoPortalUseCase gerarLink;
    private final RevogarLinkDoPortalUseCase revogarLink;
    private final ConsultarPortalQuery consultarPortal;

    PortalController(GerarLinkDoPortalUseCase gerarLink, RevogarLinkDoPortalUseCase revogarLink,
                     ConsultarPortalQuery consultarPortal) {
        this.gerarLink = gerarLink;
        this.revogarLink = revogarLink;
        this.consultarPortal = consultarPortal;
    }

    @PostMapping("/api/clientes/{id}/portal")
    LinkDoPortalResponse gerar(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return gerarLink.gerar(new GerarLinkDoPortalCommand(Identidade.escritorio(me), new ClienteId(id)));
    }

    @DeleteMapping("/api/clientes/{id}/portal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revogar(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        revogarLink.revogar(new RevogarLinkDoPortalCommand(Identidade.escritorio(me), new ClienteId(id)));
    }

    @GetMapping("/api/portal/{token}")
    PortalResponse portal(@PathVariable String token) {
        return consultarPortal.consultar(token);
    }
}
