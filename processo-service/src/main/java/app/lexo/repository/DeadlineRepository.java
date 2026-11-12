package app.lexo.repository;

import app.lexo.domain.LegacyDeadline;
import app.lexo.domain.enums.DeadlineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<LegacyDeadline, String> {

    List<LegacyDeadline> findByOrganizationIdOrderByDateAsc(String organizationId);

    Page<LegacyDeadline> findByOrganizationId(String organizationId, Pageable pageable);

    Page<LegacyDeadline> findByOrganizationIdAndCaseId(
            String organizationId, String caseId, Pageable pageable);

    long countByOrganizationIdAndStatusAndDateBetween(
            String organizationId, DeadlineStatus status, Instant inicio, Instant fim);

    long countByOrganizationIdAndStatusAndDateBefore(
            String organizationId, DeadlineStatus status, Instant limite);

    List<LegacyDeadline> findTop5ByOrganizationIdAndStatusOrderByDateAsc(
            String organizationId, DeadlineStatus status);

    List<LegacyDeadline> findByCaseIdInAndOrganizationIdOrderByDateAsc(List<String> caseIds, String organizationId);

    Optional<LegacyDeadline> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    List<LegacyDeadline> findByStatusAndNotifiedAtIsNullAndDateBetween(
            DeadlineStatus status, Instant start, Instant end);

    long deleteByCaseIdAndOrganizationId(String caseId, String organizationId);
}
