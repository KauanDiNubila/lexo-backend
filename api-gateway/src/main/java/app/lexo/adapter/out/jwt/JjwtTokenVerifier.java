package app.lexo.adapter.out.jwt;

import app.lexo.application.port.out.TokenVerifierPort;
import app.lexo.domain.Identity;
import app.lexo.domain.Session;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
class JjwtTokenVerifier implements TokenVerifierPort {

    private final JwtParser parser;

    JjwtTokenVerifier(@Value("${lexo.auth-secret}") String secret) {
        this.parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build();
    }

    @Override
    public Optional<Session> verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Identity identity = new Identity(claims.getSubject(), claims.get("organizationId", String.class),
                    claims.get("role", String.class), claims.get("name", String.class),
                    claims.get("email", String.class));
            long issuedAt = claims.getIssuedAt() == null ? 0 : claims.getIssuedAt().toInstant().getEpochSecond();
            return Optional.of(new Session(identity, issuedAt));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
