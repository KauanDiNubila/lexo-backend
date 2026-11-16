package app.lexo.adapter.out.jwt;

import app.lexo.domain.Session;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT token verifier")
class JjwtTokenVerifierTest {

    private static final String SECRET = "auth-secret-de-teste-com-no-minimo-32-bytes-aqui";

    private final JjwtTokenVerifier verifier = new JjwtTokenVerifier(SECRET);
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private final Instant issuedAt = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

    private JwtBuilder claims() {
        return Jwts.builder().subject("u-1").claim("organizationId", "org-1").claim("role", "ADMIN")
                .claim("name", "Ana").claim("email", "ana@x.com").issuedAt(Date.from(issuedAt))
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)));
    }

    @Test
    @DisplayName("a valid token becomes a session with the identity and the issue time")
    void valid() {
        Optional<Session> session = verifier.verify(claims().signWith(key).compact());

        assertThat(session).isPresent();
        assertThat(session.get().identity().userId()).isEqualTo("u-1");
        assertThat(session.get().identity().organizationId()).isEqualTo("org-1");
        assertThat(session.get().identity().role()).isEqualTo("ADMIN");
        assertThat(session.get().identity().name()).isEqualTo("Ana");
        assertThat(session.get().identity().email()).isEqualTo("ana@x.com");
        assertThat(session.get().issuedAtSeconds()).hasValue(issuedAt.getEpochSecond());
    }

    @Test
    @DisplayName("a token signed with another secret is refused")
    void otherSecret() {
        SecretKey other = Keys.hmacShaKeyFor("outro-segredo-completamente-diferente-com-32-bytes".getBytes(StandardCharsets.UTF_8));

        assertThat(verifier.verify(claims().signWith(other).compact())).isEmpty();
    }

    @Test
    @DisplayName("an expired token is refused")
    void expired() {
        String expired = claims().expiration(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .signWith(key).compact();

        assertThat(verifier.verify(expired)).isEmpty();
    }

    @Test
    @DisplayName("text that is not a token is refused")
    void garbage() {
        assertThat(verifier.verify("isto-nao-e-um-token")).isEmpty();
        assertThat(verifier.verify("")).isEmpty();
    }

    @Test
    @DisplayName("a token without an issue time gives a session without one")
    void noIssueTime() {
        String token = claims().issuedAt(null).signWith(key).compact();

        assertThat(verifier.verify(token).orElseThrow().issuedAtSeconds()).isEmpty();
    }

    @Test
    @DisplayName("a token without name or e-mail still verifies, with empty text")
    void noNameOrEmail() {
        String token = Jwts.builder().subject("u-1").claim("organizationId", "org-1").claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES))).signWith(key).compact();

        Session session = verifier.verify(token).orElseThrow();

        assertThat(session.identity().name()).isEmpty();
        assertThat(session.identity().email()).isEmpty();
    }
}
