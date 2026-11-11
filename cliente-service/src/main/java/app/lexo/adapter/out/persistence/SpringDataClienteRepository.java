package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

interface SpringDataClienteRepository
        extends JpaRepository<ClienteJpaEntity, String>, JpaSpecificationExecutor<ClienteJpaEntity> {

    List<ClienteJpaEntity> findByOrganizationIdOrderByNameAsc(String organizationId);

    long countByOrganizationId(String organizationId);

    Optional<ClienteJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    Optional<ClienteJpaEntity> findByPortalToken(String portalToken);

    boolean existsByIdAndOrganizationId(String id, String organizationId);
}
