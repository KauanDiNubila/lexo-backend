package app.lexo.controller;

import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import app.lexo.dto.AuthDtos;
import app.lexo.service.AutenticacaoService;
import app.lexo.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final String COOKIE_REFRESH = "lexo_refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AutenticacaoService authService;
    private final RefreshTokenService refreshTokenService;
    private final long validadeDias;

    public AutenticacaoController(AutenticacaoService authService,
                                  RefreshTokenService refreshTokenService,
                                  @Value("${lexo.refresh-expiration-days}") long validadeDias) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.validadeDias = validadeDias;
    }

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req,
                                          HttpServletResponse response) {
        AuthDtos.AuthResponse resposta = authService.registrar(req);
        emitirRefresh(resposta.user().id(), response);
        return resposta;
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest req,
                                       HttpServletRequest request, HttpServletResponse response) {
        AuthDtos.AuthResponse resposta = authService.login(req, ipDe(request));
        emitirRefresh(resposta.user().id(), response);
        return resposta;
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw ApiException.unauthorized(
                    ErrorCode.REFRESH_INVALIDO, "Sessão inválida. Faça login novamente.");
        }
        String userId = refreshTokenService.rotacionar(refreshToken);
        AuthDtos.AuthResponse resposta = authService.paraSessao(userId);
        emitirRefresh(userId, response);
        return resposta;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
                       HttpServletResponse response) {
        if (refreshToken != null) {
            refreshTokenService.revogar(refreshToken);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private void emitirRefresh(String userId, HttpServletResponse response) {
        String token = refreshTokenService.emitir(userId);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(token, Duration.ofDays(validadeDias)).toString());
    }

    private ResponseCookie cookie(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH)
                .maxAge(validade)
                .build();
    }

    private String ipDe(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String[] partes = encaminhado.split(",");
            return partes[partes.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
