package app.lexo.gateway;

import app.lexo.common.signing.GatewaySignature;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class AutenticacaoGatewayFilter implements GlobalFilter, Ordered {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_ORG_ID = "X-Org-Id";
    public static final String H_ROLE = "X-User-Role";
    public static final String H_NAME = "X-User-Name";
    public static final String H_EMAIL = "X-User-Email";

    private static final List<String> PUBLICAS_EXATAS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/convites/aceitar"
    );

    private static final List<String> PUBLICAS_PREFIXO = List.of(
            "/api/convites/info/",
            "/api/portal/",
            "/docs/"
    );

    private final JwtParser parser;
    private final String chaveAssinatura;

    public AutenticacaoGatewayFilter(@Value("${lexo.auth-secret}") String secret,
                                     @Value("${lexo.gateway.signing-key}") String chaveAssinatura,
                                     Environment env) {
        var perfis = Arrays.asList(env.getActiveProfiles());
        GatewaySignature.exigirChaveSegura(chaveAssinatura,
                perfis.contains("dev") || perfis.contains("test"));

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(key).build();
        this.chaveAssinatura = chaveAssinatura;
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
            h.remove(GatewaySignature.H_TIMESTAMP);
            h.remove(GatewaySignature.H_ASSINATURA);
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

            String userId = c.getSubject();
            String orgId = c.get("organizationId", String.class);
            String role = c.get("role", String.class);
            String nome = c.get("name", String.class);
            String email = c.get("email", String.class);

            String nomeCodificado = URLEncoder.encode(nome == null ? "" : nome, StandardCharsets.UTF_8);
            String emailNormalizado = email == null ? "" : email;

            long timestamp = Instant.now().getEpochSecond();
            String payload = GatewaySignature.payload(
                    userId, orgId, role, nomeCodificado, emailNormalizado, timestamp);

            mutada.header(H_USER_ID, userId)
                    .header(H_ORG_ID, orgId)
                    .header(H_ROLE, role)
                    .header(H_NAME, nomeCodificado)
                    .header(H_EMAIL, emailNormalizado)
                    .header(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp))
                    .header(GatewaySignature.H_ASSINATURA,
                            GatewaySignature.sign(chaveAssinatura, payload));

            return chain.filter(exchange.mutate().request(mutada.build()).build());
        } catch (Exception e) {
            return naoAutorizado(exchange);
        }
    }

    private boolean isPublica(String path) {
        return PUBLICAS_EXATAS.contains(path)
                || PUBLICAS_PREFIXO.stream().anyMatch(path::startsWith);
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
