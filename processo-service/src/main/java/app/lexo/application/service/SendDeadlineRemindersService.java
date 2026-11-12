package app.lexo.application.service;

import app.lexo.application.port.in.SendDeadlineRemindersUseCase;
import app.lexo.application.port.out.DeadlineReminder;
import app.lexo.application.port.out.ExecutionLockPort;
import app.lexo.application.port.out.LoadCasesByIdsPort;
import app.lexo.application.port.out.LoadDeadlinesToRemindPort;
import app.lexo.application.port.out.LoadUserContactPort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.application.port.out.SendReminderPort;
import app.lexo.application.port.out.UserContact;
import app.lexo.domain.Case;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
class SendDeadlineRemindersService implements SendDeadlineRemindersUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendDeadlineRemindersService.class);
    private static final String LOCK_KEY = "lock:cron:notify-deadlines";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration WINDOW = Duration.ofDays(3);

    private final ExecutionLockPort lockPort;
    private final LoadDeadlinesToRemindPort loadDeadlinesPort;
    private final LoadCasesByIdsPort loadCasesPort;
    private final LoadUserContactPort loadContactPort;
    private final SendReminderPort sendReminderPort;
    private final SaveDeadlinePort saveDeadlinePort;
    private final Clock clock;

    SendDeadlineRemindersService(ExecutionLockPort lockPort, LoadDeadlinesToRemindPort loadDeadlinesPort,
                                 LoadCasesByIdsPort loadCasesPort, LoadUserContactPort loadContactPort,
                                 SendReminderPort sendReminderPort, SaveDeadlinePort saveDeadlinePort,
                                 Clock clock) {
        this.lockPort = lockPort;
        this.loadDeadlinesPort = loadDeadlinesPort;
        this.loadCasesPort = loadCasesPort;
        this.loadContactPort = loadContactPort;
        this.sendReminderPort = sendReminderPort;
        this.saveDeadlinePort = saveDeadlinePort;
        this.clock = clock;
    }

    @Override
    public int sendReminders() {
        if (!lockPort.tryAcquire(LOCK_KEY, LOCK_TTL)) {
            log.info("[cron] deadline reminders are already running in another instance, skipping");
            return 0;
        }
        Instant now = clock.instant();
        List<Deadline> due = loadDeadlinesPort.pendingNotNotifiedBetween(now, now.plus(WINDOW));
        if (due.isEmpty()) {
            log.info("[cron] deadline reminders: no pending deadline in the window");
            return 0;
        }
        Map<CaseId, Case> casesById = loadCasesPort
                .byIds(due.stream().map(deadline -> deadline.data().caseId()).distinct().toList()).stream()
                .collect(Collectors.toMap(Case::id, value -> value));
        int notified = 0;
        for (Deadline deadline : due) {
            if (remind(deadline, casesById.get(deadline.data().caseId()))) {
                deadline.markNotified(now);
                saveDeadlinePort.save(deadline);
                notified++;
            }
        }
        log.info("[cron] deadline reminders: {} sent, {} skipped without recipient", notified, due.size() - notified);
        return notified;
    }

    private boolean remind(Deadline deadline, Case value) {
        UserId responsible = value == null ? null : value.data().responsibleId();
        if (responsible == null) {
            log.warn("[cron] deadline \"{}\" (case {}) has no responsible, skipping",
                    deadline.data().title(), deadline.data().caseId().value());
            return false;
        }
        UserContact contact = loadContactPort.byId(deadline.organizationId(), responsible).orElse(null);
        if (contact == null || contact.email() == null || contact.email().isBlank()) {
            log.warn("[cron] responsible {} of deadline \"{}\" not found or without e-mail, skipping",
                    responsible.value(), deadline.data().title());
            return false;
        }
        sendReminderPort.send(new DeadlineReminder(contact.name(), contact.email(), deadline.data().title(),
                deadline.data().date()));
        return true;
    }
}
