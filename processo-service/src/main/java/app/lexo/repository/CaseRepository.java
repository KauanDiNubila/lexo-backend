package app.lexo.repository;

import app.lexo.domain.Case;
import app.lexo.domain.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CaseRepository
        extends JpaRepository<Case, String>, JpaSpecificationExecutor<Case> {

    List<Case> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    Page<Case> findByOrganizationId(String organizationId, Pageable pageable);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, CaseStatus status);

    List<Case> findByClientIdAndOrganizationIdOrderByCreatedAtDesc(String clientId, String organizationId);

    List<Case> findByClientIdAndOrganizationId(String clientId, String organizationId);

    Optional<Case> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
