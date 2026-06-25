package app.lexo.repository;

import app.lexo.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {

    List<ActivityLog> findByCaseIdAndOrganizationIdOrderByCreatedAtDesc(String caseId, String organizationId);
}
