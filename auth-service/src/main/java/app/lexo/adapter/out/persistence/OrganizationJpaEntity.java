package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "organizations")
class OrganizationJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String plan;

    Instant trialEndsAt;
}
