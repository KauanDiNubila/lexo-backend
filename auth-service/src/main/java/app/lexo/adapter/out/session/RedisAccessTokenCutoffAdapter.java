package app.lexo.adapter.out.session;

import app.lexo.application.port.out.RevokeAccessTokensPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.signing.SessionCutoff;
import app.lexo.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class RedisAccessTokenCutoffAdapter implements RevokeAccessTokensPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAccessTokenCutoffAdapter.class);

    private final StringRedisTemplate redis;
    private final Duration timeToLive;

    RedisAccessTokenCutoffAdapter(StringRedisTemplate redis,
                                  @Value("${lexo.jwt-expiration-minutes}") long accessTokenMinutes) {
        this.redis = redis;
        this.timeToLive = Duration.ofMinutes(accessTokenMinutes + 1);
    }

    @Override
    public void revokeIssuedUpTo(UserId userId, Instant now) {
        try {
            redis.opsForValue().set(SessionCutoff.key(userId.value()), Long.toString(now.getEpochSecond()),
                    timeToLive);
        } catch (Exception e) {
            log.error("[session] could not record the session cutoff of user {}", userId.value(), e);
            throw new ServiceUnavailableException(
                    "Não foi possível concluir a operação agora. Tente novamente em instantes.");
        }
    }
}
