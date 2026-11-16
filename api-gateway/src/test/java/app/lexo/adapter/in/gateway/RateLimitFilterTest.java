package app.lexo.adapter.in.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rate limit filter")
class RateLimitFilterTest {

    private boolean allowed = true;
    private final List<String> clients = new ArrayList<>();
    private final RateLimitFilter filter = filterBehind(1);

    private RateLimitFilter filterBehind(int trustedProxies) {
        return new RateLimitFilter(client -> {
            clients.add(client);
            return Mono.just(allowed);
        }, trustedProxies);
    }

    private static final class Chain implements GatewayFilterChain {
        final AtomicReference<ServerWebExchange> reached = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            reached.set(exchange);
            return Mono.empty();
        }
    }

    private static MockServerHttpRequest.BaseBuilder<?> from(String address) throws Exception {
        return MockServerHttpRequest.get("/api/clientes")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName(address), 5000));
    }

    @Test
    @DisplayName("a request within the limit goes on")
    void within() throws Exception {
        var exchange = MockServerWebExchange.from(from("10.1.2.3"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(chain.reached.get()).isNotNull();
    }

    @Test
    @DisplayName("a request above the limit gets 429 and never reaches the chain")
    void above() throws Exception {
        allowed = false;
        var exchange = MockServerWebExchange.from(from("10.1.2.3"));
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chain.reached.get()).isNull();
    }

    @Test
    @DisplayName("the client is identified by its remote address")
    void keyedByAddress() throws Exception {
        filter.filter(MockServerWebExchange.from(from("10.1.2.3")), new Chain()).block();

        assertThat(clients).containsExactly("10.1.2.3");
    }

    @Test
    @DisplayName("behind a trusted proxy the client is the address the proxy appended, not what the client invented")
    void forwardedAddress() throws Exception {
        var exchange = MockServerWebExchange.from(from("10.0.0.5").header("X-Forwarded-For", "6.6.6.6, 198.51.100.9"));

        filter.filter(exchange, new Chain()).block();

        assertThat(clients).containsExactly("198.51.100.9");
    }

    @Test
    @DisplayName("without trusted proxies the forwarded header is ignored")
    void noTrustedProxies() throws Exception {
        var exchange = MockServerWebExchange.from(from("10.0.0.5").header("X-Forwarded-For", "198.51.100.9"));

        filterBehind(0).filter(exchange, new Chain()).block();

        assertThat(clients).containsExactly("10.0.0.5");
    }

    @Test
    @DisplayName("an unknown remote address shares one anonymous key")
    void unknownAddress() {
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes")), new Chain()).block();

        assertThat(clients).containsExactly("desconhecido");
    }

    @Test
    @DisplayName("a preflight goes on without being counted")
    void preflight() {
        Chain chain = new Chain();

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.options("/api/clientes")), chain).block();

        assertThat(chain.reached.get()).isNotNull();
        assertThat(clients).isEmpty();
    }
}
