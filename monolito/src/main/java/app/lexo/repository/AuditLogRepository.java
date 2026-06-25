package app.lexo.repository;

import app.lexo.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findTop200ByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
