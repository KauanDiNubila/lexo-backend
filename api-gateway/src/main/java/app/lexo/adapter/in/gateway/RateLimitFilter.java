package app.lexo.adapter.in.gateway;

import app.lexo.application.port.in.CheckRateLimitUseCase;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.beans.factory.annotation.Value;
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
    private final RemoteAddressResolver addressResolver;

    RateLimitFilter(CheckRateLimitUseCase rateLimit, @Value("${lexo.rate-limit.trusted-proxies}") int trustedProxies) {
        this.rateLimit = rateLimit;
        this.addressResolver = trustedProxies > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxies)
                : new RemoteAddressResolver() { };
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        return rateLimit.isAllowed(clientAddress(exchange)).flatMap(allowed -> {
            if (allowed) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        });
    }

    private String clientAddress(ServerWebExchange exchange) {
        InetSocketAddress remote = addressResolver.resolve(exchange);
        return remote == null || remote.getAddress() == null ? UNKNOWN_CLIENT : remote.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
