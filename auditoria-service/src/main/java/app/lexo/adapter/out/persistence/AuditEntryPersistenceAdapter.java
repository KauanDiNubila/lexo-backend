package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.LoadLatestAuditEntriesPort;
import app.lexo.application.port.out.SaveAuditEntryPort;
import app.lexo.domain.AuditEntry;
import app.lexo.domain.OrganizationId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AuditEntryPersistenceAdapter implements SaveAuditEntryPort, LoadLatestAuditEntriesPort {

    private final SpringDataAuditEntryRepository repository;

    AuditEntryPersistenceAdapter(SpringDataAuditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditEntry save(AuditEntry entry) {
        return AuditEntryMapper.toDomain(repository.save(AuditEntryMapper.toNewEntity(entry)));
    }

    @Override
    public List<AuditEntry> latest(OrganizationId organizationId, int limit) {
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByOrganizationId(organizationId.value(), page).stream()
                .map(AuditEntryMapper::toDomain)
                .toList();
    }
}
