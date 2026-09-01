package app.lexo.service;

import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import app.lexo.domain.RefreshToken;
import app.lexo.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService (unitario)")
class RefreshTokenServiceTest {

    private static final String USER_ID = "user-1";
    private static final long VALIDADE_DIAS = 30;

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService service;

    @BeforeEach
    void montarServico() {
        service = new RefreshTokenService(repo, VALIDADE_DIAS);
    }

    private RefreshToken tokenSalvoAoEmitir() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("emitir gera um token cru e persiste so o hash")
    void emitirPersisteApenasOHash() {
        String tokenCru = service.emitir(USER_ID);

        assertThat(tokenCru).isNotBlank();
        RefreshToken salvo = tokenSalvoAoEmitir();
        assertThat(salvo.getUserId()).isEqualTo(USER_ID);
        assertThat(salvo.getTokenHash()).isNotEqualTo(tokenCru);
        assertThat(salvo.getTokenHash()).hasSize(64); // SHA-256 em hex
        assertThat(salvo.getExpiresAt()).isAfter(Instant.now().plus(VALIDADE_DIAS - 1, ChronoUnit.DAYS));
        assertThat(salvo.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("dois tokens emitidos tem hashes diferentes (sao aleatorios)")
    void doisTokensSaoDiferentes() {
        String t1 = service.emitir(USER_ID);
        String t2 = service.emitir(USER_ID);

        assertThat(t1).isNotEqualTo(t2);
    }

    private RefreshToken tokenValido() {
        RefreshToken t = new RefreshToken();
        t.setUserId(USER_ID);
        t.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        return t;
    }

    @Test
    @DisplayName("rotacionar com token valido revoga o antigo e devolve o userId")
    void rotacionarTokenValido() {
        String tokenCru = "token-valido-qualquer";
        RefreshToken existente = tokenValido();
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(existente));

        String userId = service.rotacionar(tokenCru);

        assertThat(userId).isEqualTo(USER_ID);
        assertThat(existente.getRevokedAt()).isNotNull();
        verify(repo, never()).revogarTodosDoUsuario(anyString(), any());
    }

    @Test
    @DisplayName("rotacionar com token inexistente lanca REFRESH_INVALIDO")
    void rotacionarTokenInexistente() {
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotacionar("token-que-nao-existe"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(api.getCode()).isEqualTo(ErrorCode.REFRESH_INVALIDO);
                });
    }

    @Test
    @DisplayName("rotacionar com token expirado lanca REFRESH_EXPIRADO")
    void rotacionarTokenExpirado() {
        RefreshToken expirado = new RefreshToken();
        expirado.setUserId(USER_ID);
        expirado.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.rotacionar("token-expirado"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.REFRESH_EXPIRADO));

        verify(repo, never()).revogarTodosDoUsuario(anyString(), any());
    }

    @Test
    @DisplayName("reapresentar um token ja revogado revoga a familia inteira — deteccao de roubo")
    void reapresentarTokenRevogadoRevogaFamiliaInteira() {
        RefreshToken jaRevogado = tokenValido();
        jaRevogado.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(jaRevogado));
        when(repo.revogarTodosDoUsuario(eq(USER_ID), any())).thenReturn(3);

        assertThatThrownBy(() -> service.rotacionar("token-ja-usado-antes"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo(ErrorCode.REFRESH_INVALIDO));

        verify(repo, times(1)).revogarTodosDoUsuario(eq(USER_ID), any());
    }

    @Test
    @DisplayName("revogar marca apenas o token apresentado")
    void revogarMarcaOTokenApresentado() {
        RefreshToken existente = tokenValido();
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(existente));

        service.revogar("token-para-logout");

        assertThat(existente.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("revogar com token desconhecido nao lanca excecao")
    void revogarTokenDesconhecidoNaoLancaExcecao() {
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revogar("token-que-nao-existe");
        // nenhuma excecao — sucesso implicito
    }

    @Test
    @DisplayName("revogarSessoesDoUsuario delega ao repositorio")
    void revogarSessoesDoUsuarioDelega() {
        service.revogarSessoesDoUsuario(USER_ID);

        verify(repo).revogarTodosDoUsuario(eq(USER_ID), any());
    }
}
