package app.lexo.controller;

import app.lexo.dto.PortalDtos.PortalLinkResponse;
import app.lexo.dto.PortalDtos.PortalResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.PortalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal do cliente. O advogado gera o link (protegido); o cliente acessa a visao
 * publica read-only pelo token, sem login.
 */
@RestController
public class PortalController {

    private final PortalService service;

    public PortalController(PortalService service) {
        this.service = service;
    }

    /** Gera/rotaciona o link do portal de um cliente (somente autenticado). */
    @PostMapping("/api/clientes/{id}/portal")
    public PortalLinkResponse gerar(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return new PortalLinkResponse(service.gerarToken(me, id));
    }

    /** Visao publica do portal (sem autenticacao) — acessada pelo cliente via link. */
    @GetMapping("/api/portal/{token}")
    public PortalResponse portal(@PathVariable String token) {
        return service.porToken(token);
    }
}
