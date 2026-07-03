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

@RestController
public class PortalController {

    private final PortalService service;

    public PortalController(PortalService service) {
        this.service = service;
    }

    @PostMapping("/api/clientes/{id}/portal")
    public PortalLinkResponse gerar(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return new PortalLinkResponse(service.gerarToken(me, id));
    }

    @GetMapping("/api/portal/{token}")
    public PortalResponse portal(@PathVariable String token) {
        return service.porToken(token);
    }
}
