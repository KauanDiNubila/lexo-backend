package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "deadlines")
class DeadlineJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String caseId;

    @Convert(converter = DeadlineTypeConverter.class)
    @Column(nullable = false)
    DeadlineType type;

    @Convert(converter = DeadlineStatusConverter.class)
    @Column(nullable = false)
    DeadlineStatus status;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "text")
    String description;

    @Column(nullable = false)
    Instant date;

    Instant notifiedAt;
}
