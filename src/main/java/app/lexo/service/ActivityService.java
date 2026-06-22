package app.lexo.service;

import app.lexo.domain.ActivityLog;
import app.lexo.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Historico de atividades por processo, portado de lib/activity.ts. Nao-fatal, porem observavel. */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    public void log(String organizationId, String caseId, String userId, String userName, String action) {
        try {
            ActivityLog entry = new ActivityLog();
            entry.setOrganizationId(organizationId);
            entry.setCaseId(caseId);
            entry.setUserId(userId);
            entry.setUserName(userName);
            entry.setAction(action);
            repo.save(entry);
        } catch (Exception e) {
            log.error("[activity] falha ao registrar atividade no caso {}", caseId, e);
        }
    }
}
