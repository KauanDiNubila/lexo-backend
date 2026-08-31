package app.lexo.service;

import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import app.lexo.domain.RefreshToken;
import app.lexo.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repo;
    private final long validadeDias;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${lexo.refresh-expiration-days}") long validadeDias) {
        this.repo = repo;
        this.validadeDias = validadeDias;
    }

    @Transactional
    public String emitir(String userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String tokenCru = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(tokenCru));
        token.setExpiresAt(Instant.now().plus(validadeDias, ChronoUnit.DAYS));
        repo.save(token);

        return tokenCru;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public String rotacionar(String tokenCru) {
        RefreshToken atual = repo.findByTokenHash(hash(tokenCru))
                .orElseThrow(() -> ApiException.unauthorized(
                        ErrorCode.REFRESH_INVALIDO, "Sessão inválida. Faça login novamente."));

        if (atual.getRevokedAt() != null) {
            int revogados = repo.revogarTodosDoUsuario(atual.getUserId(), Instant.now());
            log.warn("[refresh] token ja revogado reapresentado para o usuario {} — "
                    + "{} sessao(oes) revogada(s) por suspeita de roubo", atual.getUserId(), revogados);
            throw ApiException.unauthorized(
                    ErrorCode.REFRESH_INVALIDO, "Sessão inválida. Faça login novamente.");
        }

        if (!atual.estaValido()) {
            throw ApiException.unauthorized(
                    ErrorCode.REFRESH_EXPIRADO, "Sessão expirada. Faça login novamente.");
        }

        atual.setRevokedAt(Instant.now());
        return atual.getUserId();
    }

    @Transactional
    public void revogar(String tokenCru) {
        repo.findByTokenHash(hash(tokenCru))
                .ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revogarSessoesDoUsuario(String userId) {
        repo.revogarTodosDoUsuario(userId, Instant.now());
    }

    private String hash(String tokenCru) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(tokenCru.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
