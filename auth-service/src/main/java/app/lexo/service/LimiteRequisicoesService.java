package app.lexo.service;

import app.lexo.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LimiteRequisicoesService {

    private static final Logger log = LoggerFactory.getLogger(LimiteRequisicoesService.class);

    private static final String PREFIXO = "ratelimit:";
    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count", Long.class);

    private final StringRedisTemplate redis;

    public LimiteRequisicoesService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean verificar(String key, int max, long windowSeconds) {
        String redisKey = PREFIXO + key;
        try {
            Long count = redis.execute(INCREMENT_WITH_TTL, List.of(redisKey), String.valueOf(windowSeconds));
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
