package app.lexo.repository;

import app.lexo.domain.Client;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class ClientSpecs {

    private ClientSpecs() {
    }

    public static Specification<Client> daOrganizacao(String organizationId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("organizationId"), organizationId);
    }

    public static Specification<Client> busca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String padrao = "%" + termo.trim().toLowerCase() + "%";
        return (raiz, consulta, cb) -> cb.or(
                List.of("name", "document", "email", "phone").stream()
                        .map(campo -> cb.like(cb.lower(raiz.get(campo)), padrao))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }
}
