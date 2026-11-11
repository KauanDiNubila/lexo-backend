package app.lexo.adapter.out.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

final class ClienteSpecs {

    private ClienteSpecs() {
    }

    static Specification<ClienteJpaEntity> doEscritorio(String escritorioId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("organizationId"), escritorioId);
    }

    static Specification<ClienteJpaEntity> busca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String padrao = "%" + termo.trim().toLowerCase() + "%";
        return (raiz, consulta, cb) -> cb.or(
                List.of("name", "document", "email", "phone").stream()
                        .map(campo -> cb.like(cb.lower(raiz.get(campo)), padrao))
                        .toArray(Predicate[]::new));
    }
}
