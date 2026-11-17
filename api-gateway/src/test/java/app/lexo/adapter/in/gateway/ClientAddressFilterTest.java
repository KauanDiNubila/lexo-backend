package app.lexo.adapter.in.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Client address filter")
class ClientAddressFilterTest {

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

    private static String forwardedAddress(Chain chain) {
        return chain.reached.get().getRequest().getHeaders().getFirst("X-Client-Address");
    }

    @Test
    @DisplayName("the address of the connection is forwarded and kept for the rest of the chain")
    void connectionAddress() throws Exception {
        Chain chain = new Chain();

        new ClientAddressFilter(0).filter(MockServerWebExchange.from(from("10.1.2.3")), chain).block();

        assertThat(forwardedAddress(chain)).isEqualTo("10.1.2.3");
        assertThat(chain.reached.get().<String>getAttribute(ClientAddressFilter.ATTRIBUTE)).isEqualTo("10.1.2.3");
    }

    @Test
    @DisplayName("behind a trusted proxy the client is the address the proxy appended, not what the client invented")
    void forwardedAddress() throws Exception {
        Chain chain = new Chain();
        var exchange = MockServerWebExchange.from(from("10.0.0.5").header("X-Forwarded-For", "6.6.6.6, 198.51.100.9"));

        new ClientAddressFilter(1).filter(exchange, chain).block();

        assertThat(forwardedAddress(chain)).isEqualTo("198.51.100.9");
    }

    @Test
    @DisplayName("without trusted proxies the forwarded header is ignored")
    void noTrustedProxies() throws Exception {
        Chain chain = new Chain();
        var exchange = MockServerWebExchange.from(from("10.0.0.5").header("X-Forwarded-For", "198.51.100.9"));

        new ClientAddressFilter(0).filter(exchange, chain).block();

        assertThat(forwardedAddress(chain)).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("an address sent by the client in X-Client-Address is replaced")
    void replacesAForgedHeader() throws Exception {
        Chain chain = new Chain();
        var exchange = MockServerWebExchange.from(from("10.1.2.3").header("X-Client-Address", "6.6.6.6"));

        new ClientAddressFilter(0).filter(exchange, chain).block();

        assertThat(chain.reached.get().getRequest().getHeaders().get("X-Client-Address")).containsExactly("10.1.2.3");
    }

    @Test
    @DisplayName("an unknown address sends no header and a forged one is removed")
    void unknownAddress() {
        Chain chain = new Chain();
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/clientes").header("X-Client-Address", "6.6.6.6"));

        new ClientAddressFilter(0).filter(exchange, chain).block();

        assertThat(chain.reached.get().getRequest().getHeaders().containsKey("X-Client-Address")).isFalse();
        assertThat(chain.reached.get().<String>getAttribute(ClientAddressFilter.ATTRIBUTE)).isEqualTo("desconhecido");
    }
}
