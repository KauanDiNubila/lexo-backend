package app.lexo.service;

import app.lexo.domain.RateHit;
import app.lexo.repository.RateHitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Rate limiting de janela deslizante persistido no Postgres, portado de lib/rate-limit.ts.
 * Falha ABERTO se o store estiver indisponivel — nunca derruba a autenticacao por causa do limiter.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final RateHitRepository repo;

    public RateLimitService(RateHitRepository repo) {
        this.repo = repo;
    }

    /**
     * @return true se a requisicao esta dentro do limite; false se deve ser bloqueada.
     */
    public boolean check(String key, int max, long windowSeconds) {
        Instant since = Instant.now().minus(windowSeconds, ChronoUnit.SECONDS);
        try {
            repo.deleteExpired(key, since);
            long count = repo.countSince(key, since);
            if (count >= max) return false;

            RateHit hit = new RateHit();
            hit.setKey(key);
            repo.save(hit);
            return true;
        } catch (Exception e) {
            log.error("[rate-limit] store indisponivel para \"{}\", liberando requisicao", key, e);
            return true;
        }
    }
}
