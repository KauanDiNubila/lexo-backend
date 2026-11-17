package app.lexo.adapter.in.web;

import app.lexo.application.port.in.RequestPasswordResetUseCase;
import app.lexo.application.port.in.ResetPasswordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/senha")
class PasswordResetController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    PasswordResetController(RequestPasswordResetUseCase requestPasswordResetUseCase,
                            ResetPasswordUseCase resetPasswordUseCase) {
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/esqueci")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.request(request.toCommand());
    }

    @PostMapping("/redefinir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.reset(request.toCommand());
    }
}
