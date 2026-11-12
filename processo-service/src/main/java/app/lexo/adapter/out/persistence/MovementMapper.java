package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.MovementData;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;

final class MovementMapper {

    private MovementMapper() {
    }

    static CaseMovement toDomain(MovementJpaEntity entity) {
        MovementData data = new MovementData(entity.title, entity.description, entity.date);
        return CaseMovement.restore(new MovementId(entity.getId()), new OrganizationId(entity.organizationId),
                new CaseId(entity.caseId), data, entity.getCreatedAt());
    }

    static void copyTo(CaseMovement movement, MovementJpaEntity entity) {
        MovementData data = movement.data();
        entity.organizationId = movement.organizationId().value();
        entity.caseId = movement.caseId().value();
        entity.title = data.title();
        entity.description = data.description();
        entity.date = data.date();
    }
}
