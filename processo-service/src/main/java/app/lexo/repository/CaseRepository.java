package app.lexo.repository;

import app.lexo.domain.LegacyCase;
import app.lexo.domain.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CaseRepository
        extends JpaRepository<LegacyCase, String>, JpaSpecificationExecutor<LegacyCase> {

    List<LegacyCase> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    Page<LegacyCase> findByOrganizationId(String organizationId, Pageable pageable);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, CaseStatus status);

    List<LegacyCase> findByClientIdAndOrganizationIdOrderByCreatedAtDesc(String clientId, String organizationId);

    List<LegacyCase> findByClientIdAndOrganizationId(String clientId, String organizationId);

    Optional<LegacyCase> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
