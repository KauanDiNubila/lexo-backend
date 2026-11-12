package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

interface SpringDataCaseRepository
        extends JpaRepository<CaseJpaEntity, String>, JpaSpecificationExecutor<CaseJpaEntity> {

    List<CaseJpaEntity> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, CaseStatus status);

    List<CaseJpaEntity> findByClientIdAndOrganizationIdOrderByCreatedAtDesc(String clientId, String organizationId);

    Optional<CaseJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
