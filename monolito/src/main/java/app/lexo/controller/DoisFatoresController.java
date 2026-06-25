package app.lexo.controller;

import app.lexo.dto.TotpDtos.CodeRequest;
import app.lexo.dto.TotpDtos.InitiateResponse;
import app.lexo.security.AuthUser;
import app.lexo.service.DoisFatoresService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Verificacao em dois fatores (TOTP). */
@RestController
@RequestMapping("/api/2fa")
public class DoisFatoresController {

    private final DoisFatoresService service;

    public DoisFatoresController(DoisFatoresService service) {
        this.service = service;
    }

    @PostMapping("/iniciar")
    public InitiateResponse initiate(@AuthenticationPrincipal AuthUser me) {
        return service.iniciar(me);
    }

    @PostMapping("/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CodeRequest req) {
        service.confirmar(me, req.code());
    }

    @PostMapping("/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CodeRequest req) {
        service.desativar(me, req.code());
    }
}
