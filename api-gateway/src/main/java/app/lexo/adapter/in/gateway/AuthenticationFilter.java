package app.lexo.adapter.in.gateway;

import app.lexo.application.port.in.AuthenticateUseCase;
import app.lexo.application.port.in.AuthenticateUseCase.Access;
import app.lexo.common.signing.GatewaySignature;
import app.lexo.domain.Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String H_USER_ID = "X-User-Id";
    private static final String H_ORG_ID = "X-Org-Id";
    private static final String H_ROLE = "X-User-Role";
    private static final String H_NAME = "X-User-Name";
    private static final String H_EMAIL = "X-User-Email";

    private static final List<String> IDENTITY_HEADERS = List.of(
            H_USER_ID, H_ORG_ID, H_ROLE, H_NAME, H_EMAIL,
            GatewaySignature.H_TIMESTAMP, GatewaySignature.H_ASSINATURA);

    private final AuthenticateUseCase authenticate;
    private final String signingKey;

    AuthenticationFilter(AuthenticateUseCase authenticate, @Value("${lexo.gateway.signing-key}") String signingKey) {
        this.authenticate = authenticate;
        this.signingKey = signingKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        ServerHttpRequest.Builder forwarded = request.mutate().headers(h -> IDENTITY_HEADERS.forEach(h::remove));
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authenticate.authenticate(request.getURI().getPath(), authorization)
                .flatMap(access -> switch (access) {
                    case Access.Public ignored -> chain.filter(exchange.mutate().request(forwarded.build()).build());
                    case Access.Granted granted -> chain.filter(
                            exchange.mutate().request(withIdentity(forwarded, granted.identity()).build()).build());
                    case Access.Denied ignored -> unauthorized(exchange);
                });
    }

    private ServerHttpRequest.Builder withIdentity(ServerHttpRequest.Builder request, Identity identity) {
        String encodedName = URLEncoder.encode(identity.name(), StandardCharsets.UTF_8);
        long timestamp = Instant.now().getEpochSecond();
        String payload = GatewaySignature.payload(identity.userId(), identity.organizationId(), identity.role(),
                encodedName, identity.email(), timestamp);
        return request
                .header(H_USER_ID, identity.userId())
                .header(H_ORG_ID, identity.organizationId())
                .header(H_ROLE, identity.role())
                .header(H_NAME, encodedName)
                .header(H_EMAIL, identity.email())
                .header(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp))
                .header(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(signingKey, payload));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
