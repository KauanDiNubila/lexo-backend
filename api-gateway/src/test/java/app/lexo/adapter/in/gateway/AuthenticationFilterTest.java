package app.lexo.adapter.in.gateway;

import app.lexo.application.port.in.AuthenticateUseCase;
import app.lexo.application.port.in.AuthenticateUseCase.Access;
import app.lexo.common.signing.GatewaySignature;
import app.lexo.domain.Identity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authentication filter")
class AuthenticationFilterTest {

    private static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";

    private Access answer = new Access.Denied();
    private final List<String> asked = new ArrayList<>();
    private final AuthenticateUseCase useCase = (path, authorization) -> {
        asked.add(path + " | " + authorization);
        return Mono.just(answer);
    };
    private final AuthenticationFilter filter = new AuthenticationFilter(useCase, SIGNING_KEY);

    private static final class Chain implements GatewayFilterChain {
        final AtomicReference<ServerWebExchange> reached = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            reached.set(exchange);
            return Mono.empty();
        }
    }

    @Test
    @DisplayName("a preflight goes straight through without asking the use case")
    void preflight() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/clientes"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(chain.reached.get()).isNotNull();
        assertThat(asked).isEmpty();
    }

    @Test
    @DisplayName("asks the use case for the path and the Authorization header")
    void asksWithPathAndAuthorization() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer abc"));

        filter.filter(exchange, new Chain()).block();

        assertThat(asked).containsExactly("/api/clientes | Bearer abc");
    }

    @Test
    @DisplayName("a denied request gets 401 and never reaches the chain")
    void denied() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.reached.get()).isNull();
    }

    @Test
    @DisplayName("a granted request carries the identity, URL-encoded and signed")
    void granted() {
        answer = new Access.Granted(new Identity("u-1", "org-1", "ADMIN", "Ana Advogada", "ana@x.com"));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        HttpHeaders forwarded = chain.reached.get().getRequest().getHeaders();
        String name = URLEncoder.encode("Ana Advogada", StandardCharsets.UTF_8);
        assertThat(forwarded.getFirst("X-User-Id")).isEqualTo("u-1");
        assertThat(forwarded.getFirst("X-Org-Id")).isEqualTo("org-1");
        assertThat(forwarded.getFirst("X-User-Role")).isEqualTo("ADMIN");
        assertThat(forwarded.getFirst("X-User-Name")).isEqualTo(name);
        assertThat(forwarded.getFirst("X-User-Email")).isEqualTo("ana@x.com");
        long timestamp = Long.parseLong(forwarded.getFirst(GatewaySignature.H_TIMESTAMP));
        assertThat(Math.abs(Instant.now().getEpochSecond() - timestamp)).isLessThan(30);
        String payload = GatewaySignature.payload("u-1", "org-1", "ADMIN", name, "ana@x.com", timestamp);
        assertThat(GatewaySignature.verify(SIGNING_KEY, payload, forwarded.getFirst(GatewaySignature.H_ASSINATURA)))
                .isTrue();
    }

    @Test
    @DisplayName("identity headers sent by the client are dropped and replaced by the granted identity")
    void forgedHeadersAreReplaced() {
        answer = new Access.Granted(new Identity("u-real", "org-1", "ADMIN", "Ana", "ana@x.com"));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes")
                .header("X-User-Id", "u-falso").header("X-User-Role", "OWNER")
                .header(GatewaySignature.H_ASSINATURA, "forjada"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        HttpHeaders forwarded = chain.reached.get().getRequest().getHeaders();
        assertThat(forwarded.get("X-User-Id")).containsExactly("u-real");
        assertThat(forwarded.get("X-User-Role")).containsExactly("ADMIN");
        assertThat(forwarded.get(GatewaySignature.H_ASSINATURA)).hasSize(1).doesNotContain("forjada");
    }

    @Test
    @DisplayName("a public request goes through with every identity header removed")
    void publicRequest() {
        answer = new Access.Public();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login")
                .header("X-User-Id", "u-falso").header("X-Org-Id", "org-falso").header("X-User-Name", "x")
                .header("X-User-Email", "x").header(GatewaySignature.H_TIMESTAMP, "1")
                .header(GatewaySignature.H_ASSINATURA, "forjada"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        HttpHeaders forwarded = chain.reached.get().getRequest().getHeaders();
        assertThat(forwarded.keySet()).doesNotContain("X-User-Id", "X-Org-Id", "X-User-Name", "X-User-Email",
                GatewaySignature.H_TIMESTAMP, GatewaySignature.H_ASSINATURA);
    }
}
