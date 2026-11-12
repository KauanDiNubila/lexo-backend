package app.lexo.adapter.in.web;

import app.lexo.application.port.in.ConfirmTwoFactorCommand;
import app.lexo.application.port.in.ConfirmTwoFactorUseCase;
import app.lexo.application.port.in.DisableTwoFactorCommand;
import app.lexo.application.port.in.DisableTwoFactorUseCase;
import app.lexo.application.port.in.StartTwoFactorCommand;
import app.lexo.application.port.in.StartTwoFactorUseCase;
import app.lexo.application.port.in.TwoFactorSetup;
import app.lexo.common.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/2fa")
class TwoFactorController {

    private final StartTwoFactorUseCase startTwoFactorUseCase;
    private final ConfirmTwoFactorUseCase confirmTwoFactorUseCase;
    private final DisableTwoFactorUseCase disableTwoFactorUseCase;

    TwoFactorController(StartTwoFactorUseCase startTwoFactorUseCase,
                        ConfirmTwoFactorUseCase confirmTwoFactorUseCase,
                        DisableTwoFactorUseCase disableTwoFactorUseCase) {
        this.startTwoFactorUseCase = startTwoFactorUseCase;
        this.confirmTwoFactorUseCase = confirmTwoFactorUseCase;
        this.disableTwoFactorUseCase = disableTwoFactorUseCase;
    }

    @PostMapping("/iniciar")
    TwoFactorSetup start(@AuthenticationPrincipal AuthUser me) {
        return startTwoFactorUseCase.start(new StartTwoFactorCommand(Identity.organization(me), Identity.user(me)));
    }

    @PostMapping("/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirm(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CodeRequest request) {
        confirmTwoFactorUseCase.confirm(
                new ConfirmTwoFactorCommand(Identity.organization(me), Identity.actor(me), request.code()));
    }

    @PostMapping("/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CodeRequest request) {
        disableTwoFactorUseCase.disable(
                new DisableTwoFactorCommand(Identity.organization(me), Identity.actor(me), request.code()));
    }
}
