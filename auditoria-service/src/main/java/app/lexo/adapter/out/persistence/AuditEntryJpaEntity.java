package app.lexo.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_org", columnList = "organizationId"),
                @Index(name = "idx_audit_created", columnList = "createdAt")
        }
)
class AuditEntryJpaEntity {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    String id;

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

    @Column(updatable = false)
    Instant createdAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
