package app.lexo.service;

import app.lexo.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimiteRequisicoesService {

    private static final Logger log = LoggerFactory.getLogger(LimiteRequisicoesService.class);

    private static final String PREFIXO = "ratelimit:";

    private final StringRedisTemplate redis;

    public LimiteRequisicoesService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean verificar(String key, int max, long windowSeconds) {
        String redisKey = PREFIXO + key;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            return count == null || count <= max;
        } catch (Exception e) {
            log.error("[rate-limit] Redis indisponivel para \"{}\", recusando a requisicao", key, e);
            throw ApiException.serviceUnavailable(
                    "Não foi possível validar o acesso agora. Tente novamente em instantes.");
        }
    }

    public void limpar(String key) {
        try {
            redis.delete(PREFIXO + key);
        } catch (Exception e) {
            log.warn("[rate-limit] falha ao limpar o contador \"{}\"", key, e);
        }
    }
}
