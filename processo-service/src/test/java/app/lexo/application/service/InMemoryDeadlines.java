package app.lexo.application.service;

import app.lexo.application.port.out.DeleteDeadlinePort;
import app.lexo.application.port.out.DeleteDeadlinesOfCasePort;
import app.lexo.application.port.out.LoadDeadlinePort;
import app.lexo.application.port.out.LoadDeadlinesToRemindPort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryDeadlines implements SaveDeadlinePort, LoadDeadlinePort, DeleteDeadlinePort,
        DeleteDeadlinesOfCasePort, LoadDeadlinesToRemindPort {

    private final Map<String, Deadline> deadlines = new LinkedHashMap<>();
    private int sequence;

    @Override
    public Deadline save(Deadline deadline) {
        Deadline saved = deadline.id() == null
                ? Deadline.restore(new DeadlineId("deadline-" + (++sequence)), deadline.organizationId(),
                        deadline.data(), deadline.status(), deadline.notifiedAt())
                : deadline;
        deadlines.put(saved.id().value(), saved);
        return saved;
    }

    @Override
    public Optional<Deadline> byId(OrganizationId organizationId, DeadlineId deadlineId) {
        return Optional.ofNullable(deadlines.get(deadlineId.value()))
                .filter(deadline -> deadline.organizationId().equals(organizationId));
    }

    @Override
    public boolean delete(OrganizationId organizationId, DeadlineId deadlineId) {
        return byId(organizationId, deadlineId)
                .map(deadline -> deadlines.remove(deadlineId.value()) != null).orElse(false);
    }

    @Override
    public void deleteOfCase(OrganizationId organizationId, CaseId caseId) {
        deadlines.values().removeIf(deadline -> deadline.organizationId().equals(organizationId)
                && deadline.data().caseId().equals(caseId));
    }

    @Override
    public List<Deadline> pendingNotNotifiedBetween(Instant from, Instant to) {
        return deadlines.values().stream()
                .filter(deadline -> deadline.status() == DeadlineStatus.PENDING && deadline.notifiedAt() == null
                        && !deadline.data().date().isBefore(from) && !deadline.data().date().isAfter(to))
                .toList();
    }

    List<Deadline> all() {
        return new ArrayList<>(deadlines.values());
    }
}
