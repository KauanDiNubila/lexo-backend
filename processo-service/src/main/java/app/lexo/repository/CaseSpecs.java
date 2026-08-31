package app.lexo.repository;

import app.lexo.domain.Case;
import app.lexo.domain.enums.CaseStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class CaseSpecs {

    private CaseSpecs() {
    }

    public static Specification<Case> daOrganizacao(String organizationId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("organizationId"), organizationId);
    }

    public static Specification<Case> comStatus(CaseStatus status) {
        if (status == null) {
            return null;
        }
        return (raiz, consulta, cb) -> cb.equal(raiz.get("status"), status);
    }

    public static Specification<Case> busca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String padrao = "%" + termo.trim().toLowerCase() + "%";
        return (raiz, consulta, cb) -> cb.or(
                List.of("number", "area").stream()
                        .map(campo -> cb.like(cb.lower(raiz.get(campo)), padrao))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }
}
