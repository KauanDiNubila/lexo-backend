package app.lexo.application.service;

import app.lexo.application.port.out.LoadLatestAuditEntriesPort;
import app.lexo.application.port.out.SaveAuditEntryPort;
import app.lexo.domain.AuditEntry;
import app.lexo.domain.AuditEntryId;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class InMemoryAuditEntries implements SaveAuditEntryPort, LoadLatestAuditEntriesPort {

    static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    private final List<AuditEntry> stored = new ArrayList<>();
    int lastLimit;

    @Override
    public AuditEntry save(AuditEntry entry) {
        AuditEntry saved = new AuditEntry(new AuditEntryId("entry-" + (stored.size() + 1)), entry.organizationId(),
                entry.userId(), entry.userName(), entry.action(), entry.entityType(), entry.entityId(),
                entry.description(), entry.occurredAt());
        stored.add(saved);
        return saved;
    }

    @Override
    public List<AuditEntry> latest(OrganizationId organizationId, int limit) {
        lastLimit = limit;
        return stored.stream()
                .filter(e -> e.organizationId().equals(organizationId))
                .sorted(Comparator.comparing(AuditEntry::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    List<AuditEntry> all() {
        return stored;
    }
}
