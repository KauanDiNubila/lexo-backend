package app.lexo.adapter.out.persistence;

import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserId;

final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        return User.restore(new UserId(entity.getId()), new OrganizationId(entity.organizationId), entity.name,
                new EmailAddress(entity.email), entity.passwordHash, entity.role, entity.totpSecret,
                entity.totpEnabled, entity.totpPendingSecret, entity.getCreatedAt());
    }

    static void copyTo(User user, UserJpaEntity entity) {
        entity.organizationId = user.organizationId().value();
        entity.name = user.name();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.totpSecret = user.twoFactorSecret();
        entity.totpEnabled = user.twoFactorEnabled();
        entity.totpPendingSecret = user.pendingTwoFactorSecret().orElse(null);
    }
}
