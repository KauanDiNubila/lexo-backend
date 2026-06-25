package app.lexo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting com Redis (contador por janela fixa via INCR + EXPIRE).
 * Muito mais rapido que o store anterior no Postgres. Falha ABERTO se o Redis estiver
 * indisponivel — nunca derruba a autenticacao por causa do limiter.
 */
@Service
public class LimiteRequisicoesService {

    private static final Logger log = LoggerFactory.getLogger(LimiteRequisicoesService.class);

    private final StringRedisTemplate redis;

    public LimiteRequisicoesService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @return true se a requisicao esta dentro do limite; false se deve ser bloqueada.
     */
    public boolean verificar(String key, int max, long windowSeconds) {
        String redisKey = "ratelimit:" + key;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                // primeira requisicao da janela: define a expiracao
                redis.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            return count == null || count <= max;
        } catch (Exception e) {
            log.error("[rate-limit] Redis indisponivel para \"{}\", liberando requisicao", key, e);
            return true;
        }
    }
}
