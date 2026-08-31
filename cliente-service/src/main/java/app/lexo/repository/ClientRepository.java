package app.lexo.repository;

import app.lexo.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ClientRepository
        extends JpaRepository<Client, String>, JpaSpecificationExecutor<Client> {

    List<Client> findByOrganizationIdOrderByNameAsc(String organizationId);

    Page<Client> findByOrganizationId(String organizationId, Pageable pageable);

    long countByOrganizationId(String organizationId);

    Optional<Client> findByIdAndOrganizationId(String id, String organizationId);

    Optional<Client> findByPortalToken(String portalToken);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
