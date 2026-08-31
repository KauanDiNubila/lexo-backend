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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService (unitario)")
class AutenticacaoServiceTest {

    private static final String EMAIL = "ana@escritorio.com";
    private static final String SENHA = "senha12345";
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

    private void credenciaisValidas(User u) {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(SENHA, "hash")).thenReturn(true);
    }

    @Test
    @DisplayName("sem 2FA, credenciais corretas devolvem token")
    void loginSemDoisFatores() {
        credenciaisValidas(usuario(false));
        when(jwtService.issue(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        var resposta = service.login(new LoginRequest(EMAIL, SENHA, null));

        assertThat(resposta.token()).isEqualTo("token-jwt");
        assertThat(resposta.user().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("com 2FA ativo e sem codigo, responde TOTP_REQUIRED")
    void loginSemCodigoPedeSegundoFator() {
        credenciaisValidas(usuario(true));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null)))
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

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, "   ")))
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

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, "000000")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.TOTP_INVALID));
    }

    @Test
    @DisplayName("com 2FA ativo e codigo correto, devolve token")
    void loginComCodigoCorreto() {
        credenciaisValidas(usuario(true));
        when(crypto.decifrar(SEGREDO_CIFRADO)).thenReturn(SEGREDO);
        when(totp.verify(SEGREDO, "123456")).thenReturn(true);
        when(jwtService.issue(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("token-jwt");

        var resposta = service.login(new LoginRequest(EMAIL, SENHA, "123456"));

        assertThat(resposta.token()).isEqualTo("token-jwt");
    }

    @Test
    @DisplayName("senha errada responde CREDENCIAIS_INVALIDAS, sem revelar o motivo")
    void senhaErrada() {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
        when(passwordEncoder.matches(SENHA, "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email ou senha inválidos")
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS));
    }

    @Test
    @DisplayName("rate limit estourado responde como credencial invalida")
    void rateLimitEstourado() {
        when(rateLimit.verificar(anyString(), anyInt(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, SENHA, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.CREDENCIAIS_INVALIDAS));
    }
}
