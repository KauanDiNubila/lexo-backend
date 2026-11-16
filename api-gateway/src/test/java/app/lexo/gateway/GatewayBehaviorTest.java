package app.lexo.gateway;

import app.lexo.common.signing.GatewaySignature;
import app.lexo.common.signing.SessionCutoff;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.testcontainers.containers.GenericContainer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway over HTTP")
class GatewayBehaviorTest extends GatewayHttpTestBase {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        configure(registry, REDIS.getHost(), REDIS.getMappedPort(6379));
    }

    @Autowired
    private ReactiveStringRedisTemplate redis;

    private int hitsBefore() {
        return DOWNSTREAM.hits();
    }

    private RequestHeadersSpec<?> get(String path, String bearer) {
        var request = web.get().uri(path);
        return bearer == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
    }

    @Test
    @DisplayName("a private route without Authorization is refused and never reaches the service")
    void noAuthorization() {
        int before = hitsBefore();

        web.get().uri("/api/clientes").exchange().expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.hits()).isEqualTo(before);
    }

    @Test
    @DisplayName("Authorization without the Bearer prefix is refused")
    void noBearerPrefix() {
        web.get().uri("/api/clientes").header(HttpHeaders.AUTHORIZATION, validToken("u-1"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("a token signed with another secret is refused")
    void otherSecret() {
        String forged = token("u-1", "Ana", "ana@x.com", Instant.now(), Instant.now().plus(15, ChronoUnit.MINUTES),
                Keys.hmacShaKeyFor("outro-segredo-completamente-diferente-com-32-bytes".getBytes(StandardCharsets.UTF_8)));

        get("/api/clientes", forged).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("an expired token is refused")
    void expired() {
        String expired = token("u-1", "Ana", "ana@x.com", Instant.now().minus(20, ChronoUnit.MINUTES),
                Instant.now().minus(5, ChronoUnit.MINUTES), jwtKey());

        get("/api/clientes", expired).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("a valid token reaches the service with signed identity headers")
    void signedIdentity() {
        String userId = "u-" + UUID.randomUUID();

        get("/api/clientes", validToken(userId)).exchange().expectStatus().isOk();

        String name = URLEncoder.encode("Ana Advogada", StandardCharsets.UTF_8);
        assertThat(DOWNSTREAM.header("X-User-Id")).isEqualTo(userId);
        assertThat(DOWNSTREAM.header("X-Org-Id")).isEqualTo("org-1");
        assertThat(DOWNSTREAM.header("X-User-Role")).isEqualTo("ADMIN");
        assertThat(DOWNSTREAM.header("X-User-Name")).isEqualTo(name);
        assertThat(DOWNSTREAM.header("X-User-Email")).isEqualTo("ana@x.com");
        long timestamp = Long.parseLong(DOWNSTREAM.header(GatewaySignature.H_TIMESTAMP));
        assertThat(Math.abs(Instant.now().getEpochSecond() - timestamp)).isLessThan(30);
        String payload = GatewaySignature.payload(userId, "org-1", "ADMIN", name, "ana@x.com", timestamp);
        assertThat(GatewaySignature.verify(SIGNING_KEY, payload, DOWNSTREAM.header(GatewaySignature.H_SIGNATURE)))
                .isTrue();
    }

    @Test
    @DisplayName("identity headers forged by the client are replaced by the ones from the token")
    void forgedHeadersAreReplaced() {
        get("/api/clientes", validToken("u-real"))
                .header("X-User-Id", "u-falso").header("X-Org-Id", "org-falso").header("X-User-Role", "OWNER")
                .header(GatewaySignature.H_SIGNATURE, "forjada")
                .exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-User-Id")).isEqualTo("u-real");
        assertThat(DOWNSTREAM.header("X-Org-Id")).isEqualTo("org-1");
        assertThat(DOWNSTREAM.header("X-User-Role")).isEqualTo("ADMIN");
        assertThat(DOWNSTREAM.header(GatewaySignature.H_SIGNATURE)).isNotEqualTo("forjada");
    }

    @Test
    @DisplayName("a missing name or e-mail in the token does not break the headers")
    void missingNameAndEmail() {
        String bare = token("u-bare", null, null, Instant.now().minus(1, ChronoUnit.MINUTES),
                Instant.now().plus(15, ChronoUnit.MINUTES), jwtKey());

        get("/api/clientes", bare).exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-User-Name")).isEmpty();
        assertThat(DOWNSTREAM.header("X-User-Email")).isEmpty();
        assertThat(DOWNSTREAM.header("X-User-Id")).isEqualTo("u-bare");
    }

    @Test
    @DisplayName("a signed token without organization or role claims still goes through, with those headers absent")
    void tokenWithoutOrganization() {
        String bare = Jwts.builder().subject("u-1").expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .signWith(jwtKey()).compact();

        get("/api/clientes", bare).exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-User-Id")).isEqualTo("u-1");
        assertThat(DOWNSTREAM.header("X-Org-Id")).isNull();
        assertThat(DOWNSTREAM.header("X-User-Role")).isNull();
    }

    @Test
    @DisplayName("an exact public route needs no token and arrives without identity headers")
    void publicExactRoute() {
        web.post().uri("/api/auth/login")
                .header("X-User-Id", "u-falso").header(GatewaySignature.H_SIGNATURE, "forjada")
                .exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-User-Id")).isNull();
        assertThat(DOWNSTREAM.header(GatewaySignature.H_SIGNATURE)).isNull();
    }

    @Test
    @DisplayName("a public prefix (client portal, invite info, docs) needs no token")
    void publicPrefixRoutes() {
        web.get().uri("/api/portal/algum-token").exchange().expectStatus().isOk();
        web.get().uri("/api/convites/info/abc").exchange().expectStatus().isOk();
        web.get().uri("/docs/auth-service/v3/api-docs").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("a path that only looks like a public one still needs a token")
    void lookalikeIsPrivate() {
        web.get().uri("/api/convites/info").exchange().expectStatus().isUnauthorized();
        web.get().uri("/api/convites/aceitar-tudo").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("a token issued before the user's session cutoff is refused")
    void issuedBeforeCutoff() {
        String userId = "u-" + UUID.randomUUID();
        redis.opsForValue().set(SessionCutoff.key(userId), Long.toString(Instant.now().getEpochSecond())).block();

        get("/api/clientes", validToken(userId)).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("a token issued after the cutoff goes through")
    void issuedAfterCutoff() {
        String userId = "u-" + UUID.randomUUID();
        redis.opsForValue().set(SessionCutoff.key(userId),
                Long.toString(Instant.now().minus(10, ChronoUnit.MINUTES).getEpochSecond())).block();

        get("/api/clientes", validToken(userId)).exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("the cutoff of another user does not affect this one")
    void cutoffIsPerUser() {
        String other = "u-" + UUID.randomUUID();
        redis.opsForValue().set(SessionCutoff.key(other), Long.toString(Instant.now().getEpochSecond())).block();

        get("/api/clientes", validToken("u-" + UUID.randomUUID())).exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("a token without an issue time is refused when the user has a cutoff")
    void noIssueTimeUnderCutoff() {
        String userId = "u-" + UUID.randomUUID();
        redis.opsForValue().set(SessionCutoff.key(userId), Long.toString(Instant.now().getEpochSecond())).block();
        String noIssueTime = token(userId, "Ana", "ana@x.com", null, Instant.now().plus(15, ChronoUnit.MINUTES),
                jwtKey());

        get("/api/clientes", noIssueTime).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("a preflight from an allowed origin is answered without a token")
    void allowedPreflight() {
        var response = preflight("/api/clientes", "http://localhost:5180");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .contains("http://localhost:5180");
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).contains("true");
    }

    @Test
    @DisplayName("a preflight from another origin is refused")
    void otherOriginPreflight() {
        var response = preflight("/api/clientes", "http://site-malicioso.com");

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
