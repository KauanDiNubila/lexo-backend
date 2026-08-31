package app.lexo.service;

import app.lexo.common.security.Role;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import app.lexo.domain.User;
import app.lexo.dto.AuthDtos.LoginRequest;
import app.lexo.repository.OrganizationRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService (unitario)")
class AutenticacaoServiceTest {

    private static final String EMAIL = "ana@escritorio.com";
    private static final String SENHA = "senha12345";
    private static final String IP = "203.0.113.10";
    private static final String SEGREDO_CIFRADO = "enc:v1:segredo-cifrado";
    private static final String SEGREDO = "SEGREDOTOTPBASE32";

    @Mock
    private UserRepository userRepo;

    @Mock
    private OrganizationRepository orgRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LimiteRequisicoesService rateLimit;

    @Mock
    private CriptografiaService crypto;

    @Mock
    private TotpProvider totp;

    @InjectMocks
    private AutenticacaoService service;

    private User usuario(boolean totpAtivo) {
        User u = new User();
        u.setId("user-1");
        u.setOrganizationId("org-1");
        u.setName("Ana");
        u.setEmail(EMAIL);
        u.setPasswordHash("hash");
        u.setRole(Role.ADMIN);
        u.setTotpEnabled(totpAtivo);
        if (totpAtivo) {
            u.setTotpSecret(SEGREDO_CIFRADO);
        }
        return u;
    }

    private void dentroDoLimite() {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong())).thenReturn(true);
    }

    private void credenciaisValidas(User u) {
        dentroDoLimite();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(SENHA, "hash")).thenReturn(true);
    }

    @Test
    @DisplayName("sem 2FA, credenciais corretas devolvem access token")
    void loginSemDoisFatores() {
        credenciaisValidas(usuario(false));
        when(jwtService.issue(ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        var resposta = service.login(new LoginRequest(EMAIL, SENHA, null), IP);

        assertThat(resposta.accessToken()).isEqualTo("token-jwt");
        assertThat(resposta.user().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("email e normalizado antes da busca")
    void emailNormalizado() {
        credenciaisValidas(usuario(false));
        when(jwtService.issue(ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        var resposta = service.login(new LoginRequest("  ANA@Escritorio.COM  ", SENHA, null), IP);

        assertThat(resposta.accessToken()).isEqualTo("token-jwt");
        verify(userRepo).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("login bem-sucedido zera os contadores de tentativa")
    void sucessoLimpaOsContadores() {
        credenciaisValidas(usuario(false));
        when(jwtService.issue(ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        service.login(new LoginRequest(EMAIL, SENHA, null), IP);

        verify(rateLimit).limpar("login:email:" + EMAIL);
        verify(rateLimit).limpar("login:ip:" + IP);
    }

    @Test
    @DisplayName("usuario inexistente ainda paga o custo do BCrypt")
    void usuarioInexistenteNaoVazaPorTempo() {
        dentroDoLimite();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS));

        verify(passwordEncoder).matches(ArgumentMatchers.eq(SENHA), contains("$2a$"));
        verify(rateLimit, never()).limpar(anyString());
    }

    @Test
    @DisplayName("Redis indisponivel recusa o login em vez de liberar")
    void rateLimitFalhaFechado() {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong()))
                .thenThrow(ApiException.serviceUnavailable("Não foi possível validar o acesso agora."));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("com 2FA ativo e sem codigo, responde TOTP_REQUIRED")
    void loginSemCodigoPedeSegundoFator() {
        credenciaisValidas(usuario(true));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(api.getCode()).isEqualTo(ErrorCode.TOTP_REQUIRED);
                });
    }

    @Test
    @DisplayName("com 2FA ativo e codigo em branco, responde TOTP_REQUIRED")
    void loginComCodigoEmBrancoPedeSegundoFator() {
        credenciaisValidas(usuario(true));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, "   "), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.TOTP_REQUIRED));
    }

    @Test
    @DisplayName("com 2FA ativo e codigo errado, responde TOTP_INVALID")
    void loginComCodigoErrado() {
        credenciaisValidas(usuario(true));
        when(crypto.decifrar(SEGREDO_CIFRADO)).thenReturn(SEGREDO);
        when(totp.verify(SEGREDO, "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, "000000"), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.TOTP_INVALID));
    }

    @Test
    @DisplayName("com 2FA ativo e codigo correto, devolve access token")
    void loginComCodigoCorreto() {
        credenciaisValidas(usuario(true));
        when(crypto.decifrar(SEGREDO_CIFRADO)).thenReturn(SEGREDO);
        when(totp.verify(SEGREDO, "123456")).thenReturn(true);
        when(jwtService.issue(ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        var resposta = service.login(new LoginRequest(EMAIL, SENHA, "123456"), IP);

        assertThat(resposta.accessToken()).isEqualTo("token-jwt");
    }

    @Test
    @DisplayName("senha errada responde CREDENCIAIS_INVALIDAS, sem revelar o motivo")
    void senhaErrada() {
        dentroDoLimite();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
        when(passwordEncoder.matches(SENHA, "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null), IP))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email ou senha inválidos")
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS));
    }

    @Test
    @DisplayName("rate limit estourado responde como credencial invalida")
    void rateLimitEstourado() {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null), IP))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS));

        verify(userRepo, never()).findByEmail(anyString());
    }
}
