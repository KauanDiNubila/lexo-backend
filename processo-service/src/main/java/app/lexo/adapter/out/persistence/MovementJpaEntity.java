package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "andamentos")
class MovementJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String caseId;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "text")
    String description;

    @Column(nullable = false)
    Instant date;
}
