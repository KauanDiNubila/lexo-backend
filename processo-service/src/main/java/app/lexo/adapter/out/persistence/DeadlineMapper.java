package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.OrganizationId;

final class DeadlineMapper {

    private DeadlineMapper() {
    }

    static Deadline toDomain(DeadlineJpaEntity entity) {
        DeadlineData data = new DeadlineData(new CaseId(entity.caseId), entity.title, entity.type, entity.date,
                entity.description);
        return Deadline.restore(new DeadlineId(entity.getId()), new OrganizationId(entity.organizationId), data,
                entity.status, entity.notifiedAt);
    }

    static void copyTo(Deadline deadline, DeadlineJpaEntity entity) {
        DeadlineData data = deadline.data();
        entity.organizationId = deadline.organizationId().value();
        entity.caseId = data.caseId().value();
        entity.type = data.type();
        entity.status = deadline.status();
        entity.title = data.title();
        entity.description = data.description();
        entity.date = data.date();
        entity.notifiedAt = deadline.notifiedAt();
    }
}
