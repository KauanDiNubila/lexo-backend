package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import app.lexo.domain.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cases")
class CaseJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String clientId;

    @Column(nullable = false)
    String number;

    String area;

    @Convert(converter = CaseStatusConverter.class)
    @Column(nullable = false)
    CaseStatus status;

    @Column(columnDefinition = "text")
    String description;

    @Column(name = "responsavel_id")
    String responsibleId;
}
