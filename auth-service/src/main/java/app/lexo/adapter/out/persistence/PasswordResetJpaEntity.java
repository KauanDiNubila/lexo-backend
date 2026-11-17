package app.lexo.adapter.out.persistence;

import app.lexo.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "password_resets")
class PasswordResetJpaEntity extends BaseEntity {

    @Column(nullable = false, length = 36)
    String userId;

    @Column(nullable = false, unique = true, length = 64)
    String tokenHash;

    @Column(nullable = false)
    Instant expiresAt;
}
