package app.lexo.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("InternalKeyFilter")
class InternalKeyFilterTest {

    private static final String CHAVE = "chave-interna-de-teste";

    private final InternalKeyFilter filter = new InternalKeyFilter(CHAVE);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("rota interna sem a chave nao autentica")
    void semChaveNaoAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("rota interna com a chave errada nao autentica")
    void chaveErradaNaoAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", "chave-errada");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("rota interna com a chave certa autentica com ROLE_INTERNAL")
    void chaveCertaAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", CHAVE);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_INTERNAL");
    }

    @Test
    @DisplayName("chave certa mas fora de /internal/** nao autentica")
    void chaveCertaForaDoPrefixoNaoAutentica() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/clientes");
        req.addHeader("X-Internal-Key", CHAVE);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("chave nao configurada (vazia) nunca autentica, mesmo com a chave certa enviada")
    void semChaveConfiguradaNuncaAutentica() throws Exception {
        InternalKeyFilter desabilitado = new InternalKeyFilter("");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        desabilitado.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }
}
