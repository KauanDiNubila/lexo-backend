package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseFilter;
import app.lexo.domain.OrganizationId;
import org.springframework.data.jpa.domain.Specification;

final class CaseSpecification {

    private CaseSpecification() {
    }

    static Specification<CaseJpaEntity> of(OrganizationId organizationId, CaseFilter filter) {
        Specification<CaseJpaEntity> specification =
                (root, query, builder) -> builder.equal(root.get("organizationId"), organizationId.value());
        if (filter.status() != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("status"), filter.status()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            String pattern = "%" + filter.search().trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("number")), pattern),
                    builder.like(builder.lower(root.get("area")), pattern)));
        }
        return specification;
    }
}
