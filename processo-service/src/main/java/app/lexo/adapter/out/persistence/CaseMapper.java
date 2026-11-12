package app.lexo.adapter.out.persistence;

import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

final class CaseMapper {

    private CaseMapper() {
    }

    static Case toDomain(CaseJpaEntity entity) {
        CaseData data = new CaseData(new ClientId(entity.clientId), entity.number, entity.area, entity.status,
                entity.description, UserId.optional(entity.responsibleId));
        return Case.restore(new CaseId(entity.getId()), new OrganizationId(entity.organizationId), data,
                entity.getCreatedAt());
    }

    static void copyTo(Case value, CaseJpaEntity entity) {
        CaseData data = value.data();
        entity.organizationId = value.organizationId().value();
        entity.clientId = data.clientId().value();
        entity.number = data.number();
        entity.area = data.area();
        entity.status = data.status();
        entity.description = data.description();
        entity.responsibleId = data.responsibleId() == null ? null : data.responsibleId().value();
    }
}
