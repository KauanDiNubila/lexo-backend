package app.lexo.adapter.out.token;

import app.lexo.application.port.out.IssueAccessTokenPort;
import app.lexo.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
class JwtAccessTokenAdapter implements IssueAccessTokenPort {

    private final SecretKey key;
    private final long expirationMinutes;
    private final Clock clock;

    JwtAccessTokenAdapter(@Value("${lexo.auth-secret}") String secret,
                          @Value("${lexo.jwt-expiration-minutes}") long expirationMinutes, Clock clock) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("lexo.auth-secret needs at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
        this.clock = clock;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.id().value())
                .claim("organizationId", user.organizationId().value())
                .claim("role", user.role().name())
                .claim("name", user.name())
                .claim("email", user.email().value())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }
}
