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
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditLogRepository repo;

    public AuditoriaService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void registrar(String organizationId, String userId, String userName,
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

    public void registrar(String organizationId, String userId, String userName, String action, String description) {
        registrar(organizationId, userId, userName, action, null, null, description);
    }
}
