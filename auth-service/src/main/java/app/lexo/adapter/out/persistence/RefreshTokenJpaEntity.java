package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String userId;

    @Column(nullable = false, unique = true, length = 64)
    String tokenHash;

    @Column(nullable = false)
    Instant expiresAt;

    Instant revokedAt;
}
