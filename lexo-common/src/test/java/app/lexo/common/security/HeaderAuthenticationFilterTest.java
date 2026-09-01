package app.lexo.common.security;

import app.lexo.common.signing.GatewaySignature;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("HeaderAuthenticationFilter")
class HeaderAuthenticationFilterTest {

    private static final String CHAVE = "chave-de-teste-com-no-minimo-32-bytes-aqui";
    private static final String OUTRA_CHAVE = "outra-chave-de-teste-com-no-minimo-32-bytes";

    private final HeaderAuthenticationFilter filter =
            new HeaderAuthenticationFilter(CHAVE, 60, new MockEnvironment());

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requisicaoAssinada(String userId, String orgId, String role,
                                                       String chave, long timestamp) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String nome = URLEncoder.encode("Ana", StandardCharsets.UTF_8);
        req.addHeader("X-User-Id", userId);
        req.addHeader("X-Org-Id", orgId);
        req.addHeader("X-User-Role", role);
        req.addHeader("X-User-Name", nome);
        req.addHeader("X-User-Email", "ana@x.com");

        String payload = GatewaySignature.payload(userId, orgId, role, nome, "ana@x.com", timestamp);
        req.addHeader(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        req.addHeader(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(chave, payload));
        return req;
    }

    @Test
    @DisplayName("sem headers de identidade, segue sem autenticar (rota publica)")
    void semIdentidadeSegueSemAutenticar() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("identidade sem assinatura nao autentica")
    void semAssinaturaNaoAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "u1");
        req.addHeader("X-Org-Id", "o1");
        req.addHeader("X-User-Role", "ADMIN");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("identidade com assinatura valida autentica com o AuthUser certo")
    void assinaturaValidaAutentica() throws Exception {
        long agora = Instant.now().getEpochSecond();
        MockHttpServletRequest req = requisicaoAssinada("u1", "o1", "ADMIN", CHAVE, agora);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AuthUser user = (AuthUser) auth.getPrincipal();
        assertThat(user.id()).isEqualTo("u1");
        assertThat(user.organizationId()).isEqualTo("o1");
        assertThat(user.role()).isEqualTo(Role.ADMIN);
        assertThat(user.name()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("assinatura feita com outra chave nao autentica")
    void assinaturaDeOutraChaveNaoAutentica() throws Exception {
        long agora = Instant.now().getEpochSecond();
        MockHttpServletRequest req = requisicaoAssinada("u1", "o1", "ADMIN", OUTRA_CHAVE, agora);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("trocar o papel depois de assinar invalida a assinatura")
    void papelAdulteradoNaoAutentica() throws Exception {
        long agora = Instant.now().getEpochSecond();
        MockHttpServletRequest req = requisicaoAssinada("u1", "o1", "ADMIN", CHAVE, agora);
        req.removeHeader("X-User-Role");
        req.addHeader("X-User-Role", "SECRETARIA");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("assinatura fora da janela de tempo nao autentica")
    void assinaturaExpiradaNaoAutentica() throws Exception {
        long antigo = Instant.now().getEpochSecond() - 600;
        MockHttpServletRequest req = requisicaoAssinada("u1", "o1", "ADMIN", CHAVE, antigo);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("papel invalido (fora do enum Role) nao autentica e nao propaga excecao")
    void papelInvalidoNaoAutentica() throws Exception {
        long agora = Instant.now().getEpochSecond();
        MockHttpServletRequest req = requisicaoAssinada("u1", "o1", "SUPER_ADMIN", CHAVE, agora);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("contexto ja autenticado (ex.: pelo InternalKeyFilter) nao e sobrescrito")
    void contextoJaAutenticadoNaoSobrescreve() throws Exception {
        var existente = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "servico-interno", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
    }
}
