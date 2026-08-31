package app.lexo.repository;

import app.lexo.domain.Deadline;
import app.lexo.domain.enums.DeadlineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<Deadline, String> {

    List<Deadline> findByOrganizationIdOrderByDateAsc(String organizationId);

    Page<Deadline> findByOrganizationId(String organizationId, Pageable pageable);

    Page<Deadline> findByOrganizationIdAndCaseId(
            String organizationId, String caseId, Pageable pageable);

    long countByOrganizationIdAndStatusAndDateBetween(
            String organizationId, DeadlineStatus status, Instant inicio, Instant fim);

    long countByOrganizationIdAndStatusAndDateBefore(
            String organizationId, DeadlineStatus status, Instant limite);

    List<Deadline> findTop5ByOrganizationIdAndStatusOrderByDateAsc(
            String organizationId, DeadlineStatus status);

    List<Deadline> findByCaseIdInAndOrganizationIdOrderByDateAsc(List<String> caseIds, String organizationId);

    Optional<Deadline> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    List<Deadline> findByStatusAndNotifiedAtIsNullAndDateBetween(
            DeadlineStatus status, Instant start, Instant end);

    long deleteByCaseIdAndOrganizationId(String caseId, String organizationId);
}
