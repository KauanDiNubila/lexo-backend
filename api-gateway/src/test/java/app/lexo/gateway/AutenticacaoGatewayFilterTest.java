package app.lexo.gateway;

import app.lexo.common.signing.GatewaySignature;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutenticacaoGatewayFilter")
class AutenticacaoGatewayFilterTest {

    private static final String AUTH_SECRET = "auth-secret-de-teste-com-no-minimo-32-bytes-aqui";
    private static final String CHAVE_ASSINATURA = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";

    private final SecretKey chaveJwt = Keys.hmacShaKeyFor(AUTH_SECRET.getBytes(StandardCharsets.UTF_8));

    private final AutenticacaoGatewayFilter filter =
            new AutenticacaoGatewayFilter(AUTH_SECRET, CHAVE_ASSINATURA, new MockEnvironment());

    private String token(String userId, String orgId, String role, String nome, String email, Instant exp) {
        return Jwts.builder()
                .subject(userId)
                .claim("organizationId", orgId)
                .claim("role", role)
                .claim("name", nome)
                .claim("email", email)
                .issuedAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .expiration(Date.from(exp))
                .signWith(chaveJwt)
                .compact();
    }

    private String tokenValido() {
        return token("user-1", "org-1", "ADMIN", "Ana Advogada", "ana@x.com",
                Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    private static final class ChainCaptor implements GatewayFilterChain {
        private final AtomicReference<ServerWebExchange> capturado = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            capturado.set(exchange);
            return Mono.empty();
        }

        ServerWebExchange exchange() {
            return capturado.get();
        }
    }

    @Test
    @DisplayName("construtor recusa a chave de assinatura default fora do profile dev")
    void construtorRecusaChaveDefaultForaDeDev() {
        assertThatThrownBy(() ->
                new AutenticacaoGatewayFilter(AUTH_SECRET, GatewaySignature.CHAVE_DEV, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("rota privada sem Authorization retorna 401 e nao chama a cadeia")
    void semAuthorizationBloqueia() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.exchange()).isNull();
    }

    @Test
    @DisplayName("Authorization sem o prefixo Bearer retorna 401")
    void authorizationSemBearerBloqueia() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes").header(HttpHeaders.AUTHORIZATION, tokenValido()));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token assinado com outro segredo retorna 401")
    void tokenDeOutroSegredoBloqueia() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outro-segredo-completamente-diferente-com-32-bytes".getBytes(StandardCharsets.UTF_8));
        String tokenForjado = Jwts.builder()
                .subject("atacante")
                .claim("organizationId", "org-x")
                .claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .signWith(outraChave)
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForjado));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token expirado retorna 401")
    void tokenExpiradoBloqueia() {
        String expirado = token("user-1", "org-1", "ADMIN", "Ana", "ana@x.com",
                Instant.now().minus(1, ChronoUnit.MINUTES));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token valido propaga os headers de identidade assinados para o downstream")
    void tokenValidoPropagaIdentidadeAssinada() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido()));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        HttpHeaders h = chain.exchange().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(h.getFirst("X-Org-Id")).isEqualTo("org-1");
        assertThat(h.getFirst("X-User-Role")).isEqualTo("ADMIN");
        assertThat(h.getFirst("X-User-Name")).isEqualTo("Ana+Advogada");
        assertThat(h.getFirst("X-User-Email")).isEqualTo("ana@x.com");

        String timestamp = h.getFirst(GatewaySignature.H_TIMESTAMP);
        String assinatura = h.getFirst(GatewaySignature.H_ASSINATURA);
        assertThat(timestamp).isNotBlank();
        assertThat(assinatura).isNotBlank();

        String payload = GatewaySignature.payload("user-1", "org-1", "ADMIN", "Ana+Advogada", "ana@x.com",
                Long.parseLong(timestamp));
        assertThat(GatewaySignature.verify(CHAVE_ASSINATURA, payload, assinatura)).isTrue();
    }

    @Test
    @DisplayName("headers de identidade forjados pelo cliente sao descartados e substituidos pelos do token")
    void headersForjadosSaoSubstituidos() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido())
                        .header("X-User-Id", "atacante")
                        .header("X-Org-Id", "org-alheia")
                        .header("X-User-Role", "ADMIN")
                        .header(GatewaySignature.H_ASSINATURA, "assinatura-forjada"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        HttpHeaders h = chain.exchange().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(h.getFirst("X-Org-Id")).isEqualTo("org-1");
        assertThat(h.getFirst(GatewaySignature.H_ASSINATURA)).isNotEqualTo("assinatura-forjada");
    }

    @Test
    @DisplayName("rota publica exata segue sem Authorization e com os headers de identidade limpos")
    void rotaPublicaExataSegueELimpaHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-User-Id", "atacante")
                        .header("X-Org-Id", "org-alheia"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        HttpHeaders h = chain.exchange().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Id")).isNull();
        assertThat(h.getFirst("X-Org-Id")).isNull();
    }

    @Test
    @DisplayName("rota publica por prefixo (portal do cliente) segue sem Authorization")
    void rotaPublicaPorPrefixoSegue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/portal/algum-token-de-convite"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("rota privada com prefixo parecido com uma publica continua exigindo autenticacao")
    void prefixoParecidoComPublicaNaoEngana() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login-mas-nao-e-essa-rota"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("requisicao OPTIONS segue direto, sem exigir Authorization")
    void optionsSegueDireto() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/clientes"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("nome e email ausentes no token nao quebram a propagacao dos headers")
    void nomeEEmailAusentesNaoQuebram() {
        String semNomeEEmail = Jwts.builder()
                .subject("user-1")
                .claim("organizationId", "org-1")
                .claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .signWith(chaveJwt)
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + semNomeEEmail));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        HttpHeaders h = chain.exchange().getRequest().getHeaders();
        assertThat(h.getFirst("X-User-Name")).isEqualTo("");
        assertThat(h.getFirst("X-User-Email")).isEqualTo("");
    }
}
