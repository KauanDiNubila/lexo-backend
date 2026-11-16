package app.lexo.adapter.in.gateway;

import app.lexo.application.port.in.CheckRateLimitUseCase;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String UNKNOWN_CLIENT = "desconhecido";

    private final CheckRateLimitUseCase rateLimit;

    RateLimitFilter(CheckRateLimitUseCase rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        return rateLimit.isAllowed(clientAddress(request)).flatMap(allowed -> {
            if (allowed) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        });
    }

    private String clientAddress(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null || remote.getAddress() == null ? UNKNOWN_CLIENT : remote.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
