package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_org", columnList = "organizationId"),
                @Index(name = "idx_audit_created", columnList = "createdAt")
        }
)
class AuditEntryJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    String userId;

    @Column(nullable = false)
    String userName;

    @Column(nullable = false)
    String action;

    String entityType;

    String entityId;

    @Column(nullable = false, columnDefinition = "text")
    String description;
}
