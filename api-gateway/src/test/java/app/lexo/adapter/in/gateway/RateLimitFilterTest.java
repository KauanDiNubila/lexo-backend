package app.lexo.adapter.in.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rate limit filter")
class RateLimitFilterTest {

    private boolean allowed = true;
    private final List<String> clients = new ArrayList<>();
    private final RateLimitFilter filter = new RateLimitFilter(client -> {
        clients.add(client);
        return Mono.just(allowed);
    });

    private static final class Chain implements GatewayFilterChain {
        final AtomicReference<ServerWebExchange> reached = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            reached.set(exchange);
            return Mono.empty();
        }
    }

    private static MockServerWebExchange requestFrom(String address) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/clientes"));
        if (address != null) {
            exchange.getAttributes().put(ClientAddressFilter.ATTRIBUTE, address);
        }
        return exchange;
    }

    @Test
    @DisplayName("a request within the limit goes on")
    void within() {
        var exchange = requestFrom("10.1.2.3");
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(chain.reached.get()).isNotNull();
    }

    @Test
    @DisplayName("a request above the limit gets 429 and never reaches the chain")
    void above() {
        allowed = false;
        var exchange = requestFrom("10.1.2.3");
        Chain chain = new Chain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chain.reached.get()).isNull();
    }

    @Test
    @DisplayName("the client is the address resolved earlier in the chain")
    void keyedByTheResolvedAddress() {
        filter.filter(requestFrom("198.51.100.9"), new Chain()).block();

        assertThat(clients).containsExactly("198.51.100.9");
    }

    @Test
    @DisplayName("a request without a resolved address shares one anonymous key")
    void unknownAddress() {
        filter.filter(requestFrom(null), new Chain()).block();

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
