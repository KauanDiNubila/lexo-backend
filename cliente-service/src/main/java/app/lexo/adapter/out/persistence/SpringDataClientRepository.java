package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

interface SpringDataClientRepository
        extends JpaRepository<ClientJpaEntity, String>, JpaSpecificationExecutor<ClientJpaEntity> {

    List<ClientJpaEntity> findByOrganizationIdOrderByNameAsc(String organizationId);

    long countByOrganizationId(String organizationId);

    Optional<ClientJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    Optional<ClientJpaEntity> findByPortalToken(String portalToken);

    boolean existsByIdAndOrganizationId(String id, String organizationId);
}
