package app.lexo.adapter.out.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

final class ClientSpecs {

    private ClientSpecs() {
    }

    static Specification<ClientJpaEntity> ofOrganization(String organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    static Specification<ClientJpaEntity> search(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                List.of("name", "document", "email", "phone").stream()
                        .map(field -> cb.like(cb.lower(root.get(field)), pattern))
                        .toArray(Predicate[]::new));
    }
}
