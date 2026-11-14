package app.lexo.adapter.out.persistence;

import app.lexo.domain.AuditEntry;
import app.lexo.domain.AuditEntryId;
import app.lexo.domain.OrganizationId;

final class AuditEntryMapper {

    private AuditEntryMapper() {
    }

    static AuditEntry toDomain(AuditEntryJpaEntity entity) {
        return new AuditEntry(new AuditEntryId(entity.id), new OrganizationId(entity.organizationId),
                entity.userId, entity.userName, entity.action, entity.entityType, entity.entityId,
                entity.description, entity.createdAt);
    }

    static AuditEntryJpaEntity toNewEntity(AuditEntry entry) {
        AuditEntryJpaEntity entity = new AuditEntryJpaEntity();
        entity.organizationId = entry.organizationId().value();
        entity.userId = entry.userId();
        entity.userName = entry.userName();
        entity.action = entry.action();
        entity.entityType = entry.entityType();
        entity.entityId = entry.entityId();
        entity.description = entry.description();
        entity.createdAt = entry.occurredAt();
        return entity;
    }
}
