package app.lexo.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
abstract class GatewayHttpTestBase {

    protected static final String AUTH_SECRET = "auth-secret-de-teste-com-no-minimo-32-bytes-aqui";
    protected static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";

    protected static final Downstream DOWNSTREAM = Downstream.start();

    private final SecretKey jwtKey = Keys.hmacShaKeyFor(AUTH_SECRET.getBytes(StandardCharsets.UTF_8));

    @Autowired
    protected WebTestClient web;

    @LocalServerPort
    private int port;

    protected HttpResponse<String> preflight(String path, String origin) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", origin).header("Access-Control-Request-Method", "GET").build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static void configure(DynamicPropertyRegistry registry, String redisHost, int redisPort) {
        registry.add("lexo.auth-secret", () -> AUTH_SECRET);
        registry.add("lexo.gateway.signing-key", () -> SIGNING_KEY);
        registry.add("eureka.client.enabled", () -> "false");
        for (String service : new String[]{"auth-service", "cliente-service"}) {
            registry.add("spring.cloud.discovery.client.simple.instances." + service + "[0].uri", DOWNSTREAM::url);
        }
        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> redisPort);
    }

    protected String token(String userId, String name, String email, Instant issuedAt, Instant expiresAt,
                           SecretKey key) {
        var builder = Jwts.builder().subject(userId)
                .claim("organizationId", "org-1").claim("role", "ADMIN");
        if (name != null) {
            builder.claim("name", name);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (issuedAt != null) {
            builder.issuedAt(Date.from(issuedAt));
        }
        return builder.expiration(Date.from(expiresAt)).signWith(key).compact();
    }

    protected String validToken(String userId) {
        return token(userId, "Ana Advogada", "ana@x.com", Instant.now().minus(1, ChronoUnit.MINUTES),
                Instant.now().plus(15, ChronoUnit.MINUTES), jwtKey);
    }

    protected SecretKey jwtKey() {
        return jwtKey;
    }

    protected static final class Downstream {
        private final HttpServer server;
        private final AtomicInteger hits = new AtomicInteger();
        private final Map<String, String> lastHeaders = new ConcurrentHashMap<>();

        private Downstream(HttpServer server) {
            this.server = server;
        }

        static Downstream start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                Downstream downstream = new Downstream(server);
                server.createContext("/", exchange -> {
                    downstream.hits.incrementAndGet();
                    downstream.lastHeaders.clear();
                    exchange.getRequestHeaders().forEach((k, v) -> downstream.lastHeaders.put(k.toLowerCase(), v.get(0)));
                    byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                server.start();
                return downstream;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        int hits() {
            return hits.get();
        }

        String header(String name) {
            return lastHeaders.get(name.toLowerCase());
        }
    }
}
