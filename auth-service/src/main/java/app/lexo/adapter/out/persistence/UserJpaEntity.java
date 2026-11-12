package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import app.lexo.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    String totpSecret;

    @Column(nullable = false)
    boolean totpEnabled;

    String totpPendingSecret;
}
