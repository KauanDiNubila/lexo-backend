package app.lexo.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
public class LimiteTaxaGatewayFilter implements GlobalFilter, Ordered {

    private static final String ROTA_GLOBAL = "gateway-global";

    private final RedisRateLimiter limitador;

    public LimiteTaxaGatewayFilter(RedisRateLimiter limitador,
                                    @Value("${lexo.rate-limit.replenish-rate}") int replenishRate,
                                    @Value("${lexo.rate-limit.burst-capacity}") int burstCapacity,
                                    @Value("${lexo.rate-limit.requested-tokens}") int requestedTokens) {
        this.limitador = limitador;
        limitador.getConfig().put(ROTA_GLOBAL,
                new RedisRateLimiter.Config()
                        .setReplenishRate(replenishRate)
                        .setBurstCapacity(burstCapacity)
                        .setRequestedTokens(requestedTokens));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();

        if (req.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        return limitador.isAllowed(ROTA_GLOBAL, enderecoCliente(req))
                .flatMap(resultado -> {
                    if (resultado.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                });
    }

    private String enderecoCliente(ServerHttpRequest req) {
        InetSocketAddress remoto = req.getRemoteAddress();
        return remoto == null || remoto.getAddress() == null
                ? "desconhecido"
                : remoto.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
