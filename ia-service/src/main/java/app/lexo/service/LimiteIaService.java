package app.lexo.service;

import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimiteIaService {

    private static final Logger log = LoggerFactory.getLogger(LimiteIaService.class);

    private static final Duration JANELA = Duration.ofMinutes(1);

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
            total = redis.opsForValue().increment(chave);
            if (total != null && total == 1L) {
                redis.expire(chave, JANELA);
            }
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
