package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.DeletePasswordResetsPort;
import app.lexo.application.port.out.LoadPasswordResetPort;
import app.lexo.application.port.out.SavePasswordResetPort;
import app.lexo.domain.PasswordReset;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class PasswordResetPersistenceAdapter implements SavePasswordResetPort, LoadPasswordResetPort,
        DeletePasswordResetsPort {

    private final SpringDataPasswordResetRepository repository;

    PasswordResetPersistenceAdapter(SpringDataPasswordResetRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(PasswordReset reset) {
        PasswordResetJpaEntity entity = new PasswordResetJpaEntity();
        entity.userId = reset.userId().value();
        entity.tokenHash = reset.tokenHash();
        entity.expiresAt = reset.expiresAt();
        repository.save(entity);
    }

    @Override
    public Optional<PasswordReset> byTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PasswordResetPersistenceAdapter::toDomain);
    }

    @Override
    public void ofUser(UserId userId) {
        repository.deleteByUserId(userId.value());
    }

    @Override
    public void expiredOf(UserId userId, Instant now) {
        repository.deleteByUserIdAndExpiresAtBefore(userId.value(), now);
    }

    private static PasswordReset toDomain(PasswordResetJpaEntity entity) {
        return PasswordReset.restore(new UserId(entity.userId), entity.tokenHash, entity.expiresAt);
    }
}
