package app.lexo.service;

import app.lexo.domain.AuditLog;
import app.lexo.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Log de auditoria, portado de lib/audit.ts. A falha de auditoria nao quebra o fluxo
 * principal, mas e sempre observavel (nunca silenciosa).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String organizationId, String userId, String userName,
                    String action, String entityType, String entityId, String description) {
        try {
            AuditLog entry = new AuditLog();
            entry.setOrganizationId(organizationId);
            entry.setUserId(userId);
            entry.setUserName(userName);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDescription(description);
            repo.save(entry);
        } catch (Exception e) {
            log.error("[audit] falha ao registrar acao \"{}\" (org {})", action, organizationId, e);
        }
    }

    public void log(String organizationId, String userId, String userName, String action, String description) {
        log(organizationId, userId, userName, action, null, null, description);
    }
}
