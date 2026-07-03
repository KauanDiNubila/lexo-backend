package app.lexo.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AutenticacaoGatewayFilter implements GlobalFilter, Ordered {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_ORG_ID = "X-Org-Id";
    public static final String H_ROLE = "X-User-Role";
    public static final String H_NAME = "X-User-Name";
    public static final String H_EMAIL = "X-User-Email";

    private static final List<String> PUBLICAS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/convites/aceitar",
            "/api/convites/info/",
            "/api/health",
            "/api/cron/",
            "/api/portal/"
    );

    private final JwtParser parser;

    public AutenticacaoGatewayFilter(@Value("${lexo.auth-secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();

        if (req.getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = req.getURI().getPath();

        ServerHttpRequest.Builder mutada = req.mutate().headers(h -> {
            h.remove(H_USER_ID);
            h.remove(H_ORG_ID);
            h.remove(H_ROLE);
            h.remove(H_NAME);
            h.remove(H_EMAIL);
        });

        if (isPublica(path)) {
            return chain.filter(exchange.mutate().request(mutada.build()).build());
        }

        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return naoAutorizado(exchange);
        }

        try {
            Claims c = parser.parseSignedClaims(auth.substring(7)).getPayload();
            String nome = c.get("name", String.class);
            String email = c.get("email", String.class);
            mutada.header(H_USER_ID, c.getSubject())
                    .header(H_ORG_ID, c.get("organizationId", String.class))
                    .header(H_ROLE, c.get("role", String.class))

                    .header(H_NAME, URLEncoder.encode(nome == null ? "" : nome, StandardCharsets.UTF_8))
                    .header(H_EMAIL, email == null ? "" : email);
            return chain.filter(exchange.mutate().request(mutada.build()).build());
        } catch (Exception e) {
            return naoAutorizado(exchange);
        }
    }

    private boolean isPublica(String path) {
        return PUBLICAS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> naoAutorizado(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {

        return -1;
    }
}
