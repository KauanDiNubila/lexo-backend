package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import app.lexo.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_invites")
class InviteJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Column(nullable = false, unique = true)
    String token;

    @Column(nullable = false)
    Instant expiresAt;

    Instant acceptedAt;
}
