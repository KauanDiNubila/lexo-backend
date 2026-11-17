package app.lexo.adapter.in.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
class ClientAddressFilter implements GlobalFilter, Ordered {

    static final String HEADER = "X-Client-Address";
    static final String ATTRIBUTE = "lexo.client-address";
    static final String UNKNOWN_CLIENT = "desconhecido";

    private final RemoteAddressResolver addressResolver;

    ClientAddressFilter(@Value("${lexo.rate-limit.trusted-proxies}") int trustedProxies) {
        this.addressResolver = trustedProxies > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxies)
                : new RemoteAddressResolver() { };
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress remote = addressResolver.resolve(exchange);
        String address = remote == null || remote.getAddress() == null ? null : remote.getAddress().getHostAddress();
        exchange.getAttributes().put(ATTRIBUTE, address == null ? UNKNOWN_CLIENT : address);

        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HEADER);
            if (address != null) {
                headers.set(HEADER, address);
            }
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
