package app.lexo.adapter.in.web;

import app.lexo.application.port.in.LoginUseCase;
import app.lexo.application.port.in.LogoutCommand;
import app.lexo.application.port.in.LogoutUseCase;
import app.lexo.application.port.in.RefreshSessionCommand;
import app.lexo.application.port.in.RefreshSessionUseCase;
import app.lexo.application.port.in.RegisterOrganizationUseCase;
import app.lexo.application.port.in.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterOrganizationUseCase registerOrganizationUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshCookie refreshCookie;

    AuthController(RegisterOrganizationUseCase registerOrganizationUseCase, LoginUseCase loginUseCase,
                   RefreshSessionUseCase refreshSessionUseCase, LogoutUseCase logoutUseCase,
                   RefreshCookie refreshCookie) {
        this.registerOrganizationUseCase = registerOrganizationUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshCookie = refreshCookie;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return startSession(registerOrganizationUseCase.register(request.toCommand()), response);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                       HttpServletResponse response) {
        return startSession(loginUseCase.login(request.toCommand(clientAddress(httpRequest))), response);
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken,
                         HttpServletResponse response) {
        return startSession(refreshSessionUseCase.refresh(new RefreshSessionCommand(refreshToken)), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken,
                HttpServletResponse response) {
        logoutUseCase.logout(new LogoutCommand(refreshToken));
        refreshCookie.clear(response);
    }

    private AuthResponse startSession(SessionResponse session, HttpServletResponse response) {
        refreshCookie.attach(response, session.refreshToken());
        return AuthResponse.of(session);
    }

    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
