package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "clients", indexes = @Index(name = "idx_client_org", columnList = "organizationId"))
class ClientJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String name;

    String document;

    String email;

    String phone;

    @Column(columnDefinition = "text")
    String notes;

    @Column(unique = true)
    String portalToken;

    Instant portalTokenExpiresAt;
}
