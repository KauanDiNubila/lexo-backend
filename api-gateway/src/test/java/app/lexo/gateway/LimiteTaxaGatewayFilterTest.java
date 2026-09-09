package app.lexo.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LimiteTaxaGatewayFilter")
class LimiteTaxaGatewayFilterTest {

    @Mock
    private RedisRateLimiter limitador;

    private LimiteTaxaGatewayFilter filter;

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

    @BeforeEach
    void setUp() {
        when(limitador.getConfig()).thenReturn(new HashMap<>());
        filter = new LimiteTaxaGatewayFilter(limitador, 30, 60, 1);
        clearInvocations(limitador);
    }

    private MockServerWebExchange requisicaoDe(String ip) {
        MockServerHttpRequest req = MockServerHttpRequest.get("/api/clientes")
                .remoteAddress(new InetSocketAddress(ip, 12345))
                .build();
        return MockServerWebExchange.from(req);
    }

    @Test
    @DisplayName("dentro do limite, segue a cadeia normalmente")
    void dentroDoLimiteSegueACadeia() {
        when(limitador.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
        MockServerWebExchange exchange = requisicaoDe("10.0.0.5");
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
    }

    @Test
    @DisplayName("acima do limite, retorna 429 e nao chama a cadeia")
    void acimaDoLimiteBloqueia() {
        when(limitador.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of())));
        MockServerWebExchange exchange = requisicaoDe("10.0.0.5");
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(chain.exchange()).isNull();
    }

    @Test
    @DisplayName("usa o IP remoto do cliente como chave do limitador")
    void usaIpRemotoComoChave() {
        when(limitador.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
        MockServerWebExchange exchange = requisicaoDe("203.0.113.7");
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        verify(limitador).isAllowed(eq("gateway-global"), eq("203.0.113.7"));
    }

    @Test
    @DisplayName("requisicao OPTIONS segue direto, sem consultar o limitador")
    void optionsSegueDireto() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/clientes"));
        ChainCaptor chain = new ChainCaptor();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        verifyNoInteractions(limitador);
    }
}
