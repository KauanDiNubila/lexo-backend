package app.lexo.repository;

import app.lexo.domain.Deadline;
import app.lexo.domain.enums.DeadlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeadlineRepository extends JpaRepository<Deadline, String> {

    List<Deadline> findByOrganizationIdOrderByDateAsc(String organizationId);

    Optional<Deadline> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    /** Prazos pendentes nao notificados que vencem ate a data informada (cron de notificacao). */
    List<Deadline> findByStatusAndNotifiedAtIsNullAndDateBetween(
            DeadlineStatus status, Instant start, Instant end);
}
