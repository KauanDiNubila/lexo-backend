package app.lexo.service;

import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LimiteIaService {

    private static final Logger log = LoggerFactory.getLogger(LimiteIaService.class);

    private static final Duration JANELA = Duration.ofMinutes(1);
    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count", Long.class);

    private final StringRedisTemplate redis;
    private final int limitePorMinuto;

    public LimiteIaService(StringRedisTemplate redis,
                           @Value("${lexo.ia.limite-por-minuto}") int limitePorMinuto) {
        this.redis = redis;
        this.limitePorMinuto = limitePorMinuto;
    }

    public void exigirDentroDoLimite(String organizationId) {
        String chave = "ratelimit:ia:" + organizationId;
        Long total;
        try {
            total = redis.execute(INCREMENT_WITH_TTL, List.of(chave), String.valueOf(JANELA.toSeconds()));
        } catch (Exception e) {
            log.error("[ia] Redis indisponivel para a organizacao {}, recusando a chamada",
                    organizationId, e);
            throw ApiException.serviceUnavailable(
                    "Lexo IA temporariamente indisponível. Tente novamente em instantes.");
        }

        if (total != null && total > limitePorMinuto) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCode.REQUISICOES_EM_EXCESSO,
                    "Muitas requisições de IA. Tente novamente em instantes.");
        }
    }
}
