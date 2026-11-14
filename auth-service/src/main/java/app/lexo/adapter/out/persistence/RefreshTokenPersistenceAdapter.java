package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.DeleteExpiredRefreshTokensPort;
import app.lexo.application.port.out.LoadRefreshTokenPort;
import app.lexo.application.port.out.RevokeUserRefreshTokensPort;
import app.lexo.application.port.out.SaveRefreshTokenPort;
import app.lexo.domain.RefreshToken;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class RefreshTokenPersistenceAdapter implements SaveRefreshTokenPort, LoadRefreshTokenPort,
        RevokeUserRefreshTokensPort, DeleteExpiredRefreshTokensPort {

    private final SpringDataRefreshTokenRepository repository;

    RefreshTokenPersistenceAdapter(SpringDataRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = token.id() == null
                ? new RefreshTokenJpaEntity()
                : repository.findById(token.id()).orElseThrow(
                        () -> new IllegalStateException("Refresh token " + token.id() + " does not exist to update"));
        entity.userId = token.userId().value();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.revokedAt = token.revokedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RefreshToken> byHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public int revokeAll(UserId userId, Instant now) {
        return repository.revokeAllOfUser(userId.value(), now);
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return repository.deleteExpiredBefore(cutoff);
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), new UserId(entity.userId), entity.tokenHash, entity.expiresAt,
                entity.revokedAt);
    }
}
