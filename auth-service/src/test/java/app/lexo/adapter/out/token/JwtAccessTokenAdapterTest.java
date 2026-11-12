package app.lexo.adapter.out.token;

import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtAccessTokenAdapter")
class JwtAccessTokenAdapterTest {

    private static final String SECRET = "secret-for-the-jwt-adapter-with-32-bytes-or-more";
    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");

    private final JwtAccessTokenAdapter adapter = new JwtAccessTokenAdapter(SECRET, 15,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private User user() {
        return User.restore(new UserId("user-1"), new OrganizationId("org-1"), "Ana",
                new EmailAddress("ana@escritorio.com"), "hash", Role.ADVOGADO, null, false, null, null);
    }

    private Claims parse(String token, String secret) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).clock(
                () -> java.util.Date.from(NOW)).build().parseSignedClaims(token).getPayload();
    }

    @Test
    @DisplayName("the token carries the user identity and expires after the configured minutes")
    void carriesClaims() {
        Claims claims = parse(adapter.issue(user()), SECRET);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("organizationId", String.class)).isEqualTo("org-1");
        assertThat(claims.get("role", String.class)).isEqualTo("ADVOGADO");
        assertThat(claims.get("name", String.class)).isEqualTo("Ana");
        assertThat(claims.get("email", String.class)).isEqualTo("ana@escritorio.com");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("the token is not valid with another secret")
    void signedWithTheSecret() {
        String token = adapter.issue(user());

        assertThatThrownBy(() -> parse(token, "another-secret-for-the-jwt-adapter-with-32-bytes"))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("a secret shorter than 32 bytes is refused")
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtAccessTokenAdapter("short", 15, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
