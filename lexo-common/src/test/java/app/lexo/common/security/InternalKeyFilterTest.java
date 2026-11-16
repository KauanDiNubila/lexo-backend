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

    private static final String KEY = "chave-interna-de-teste";

    private final InternalKeyFilter filter = new InternalKeyFilter(KEY);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("an internal route without the key does not authenticate")
    void withoutKeyDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("an internal route with the wrong key does not authenticate")
    void wrongKeyDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", "chave-errada");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an internal route with the right key authenticates with ROLE_INTERNAL")
    void rightKeyAuthenticates() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", KEY);
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
    @DisplayName("the right key outside /internal/** does not authenticate")
    void rightKeyOutsidePrefixDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/clientes");
        req.addHeader("X-Internal-Key", KEY);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an unconfigured (blank) key never authenticates, even if a key is sent")
    void unconfiguredKeyNeverAuthenticates() throws Exception {
        InternalKeyFilter disabled = new InternalKeyFilter("");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/clientes/1/existe");
        req.addHeader("X-Internal-Key", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        disabled.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }
}
