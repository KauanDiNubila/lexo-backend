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

    private static final String KEY = "key-de-teste-com-no-minimo-32-bytes-aqui";
    private static final String OTHER_KEY = "outra-key-de-teste-com-no-minimo-32-bytes";

    private final HeaderAuthenticationFilter filter =
            new HeaderAuthenticationFilter(KEY, 60, new MockEnvironment());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest signedRequest(String userId, String orgId, String role,
                                                       String key, long timestamp) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String name = URLEncoder.encode("Ana", StandardCharsets.UTF_8);
        req.addHeader("X-User-Id", userId);
        req.addHeader("X-Org-Id", orgId);
        req.addHeader("X-User-Role", role);
        req.addHeader("X-User-Name", name);
        req.addHeader("X-User-Email", "ana@x.com");

        String payload = GatewaySignature.payload(userId, orgId, role, name, "ana@x.com", timestamp);
        req.addHeader(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        req.addHeader(GatewaySignature.H_SIGNATURE, GatewaySignature.sign(key, payload));
        return req;
    }

    @Test
    @DisplayName("without identity headers it goes on unauthenticated (public route)")
    void withoutIdentityGoesOnUnauthenticated() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("an identity without a signature does not authenticate")
    void withoutSignatureDoesNotAuthenticate() throws Exception {
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
    @DisplayName("an identity with a valid signature authenticates with the right AuthUser")
    void validSignatureAuthenticates() throws Exception {
        long now = Instant.now().getEpochSecond();
        MockHttpServletRequest req = signedRequest("u1", "o1", "ADMIN", KEY, now);
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
    @DisplayName("a signature made with another key does not authenticate")
    void otherKeySignatureDoesNotAuthenticate() throws Exception {
        long now = Instant.now().getEpochSecond();
        MockHttpServletRequest req = signedRequest("u1", "o1", "ADMIN", OTHER_KEY, now);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("changing the role after signing invalidates the signature")
    void tamperedRoleDoesNotAuthenticate() throws Exception {
        long now = Instant.now().getEpochSecond();
        MockHttpServletRequest req = signedRequest("u1", "o1", "ADMIN", KEY, now);
        req.removeHeader("X-User-Role");
        req.addHeader("X-User-Role", "SECRETARIA");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a signature outside the time window does not authenticate")
    void expiredSignatureDoesNotAuthenticate() throws Exception {
        long old = Instant.now().getEpochSecond() - 600;
        MockHttpServletRequest req = signedRequest("u1", "o1", "ADMIN", KEY, old);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an invalid role (not in the Role enum) does not authenticate and does not throw")
    void invalidRoleDoesNotAuthenticate() throws Exception {
        long now = Instant.now().getEpochSecond();
        MockHttpServletRequest req = signedRequest("u1", "o1", "SUPER_ADMIN", KEY, now);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("an already authenticated context (for example by the InternalKeyFilter) is not overwritten")
    void alreadyAuthenticatedContextIsKept() throws Exception {
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
