package app.lexo.adapter.out.persistence;

import app.lexo.domain.DeadlineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface SpringDataDeadlineRepository extends JpaRepository<DeadlineJpaEntity, String> {

    Page<DeadlineJpaEntity> findByOrganizationId(String organizationId, Pageable pageable);

    Page<DeadlineJpaEntity> findByOrganizationIdAndCaseId(String organizationId, String caseId, Pageable pageable);

    long countByOrganizationIdAndStatusAndDateBetween(String organizationId, DeadlineStatus status, Instant start,
                                                      Instant end);

    long countByOrganizationIdAndStatusAndDateBefore(String organizationId, DeadlineStatus status, Instant limit);

    List<DeadlineJpaEntity> findByOrganizationIdAndStatusOrderByDateAsc(String organizationId, DeadlineStatus status,
                                                                        Pageable pageable);

    List<DeadlineJpaEntity> findByCaseIdInAndOrganizationIdOrderByDateAsc(Collection<String> caseIds,
                                                                          String organizationId);

    Optional<DeadlineJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    List<DeadlineJpaEntity> findByStatusAndNotifiedAtIsNullAndDateBetween(DeadlineStatus status, Instant start,
                                                                          Instant end);

    long deleteByCaseIdAndOrganizationId(String caseId, String organizationId);
}
