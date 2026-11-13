package app.lexo.application.service;

import app.lexo.application.port.in.RegisterAuditEntryCommand;
import app.lexo.application.port.in.RegisterAuditEntryUseCase;
import app.lexo.application.port.out.SaveAuditEntryPort;
import app.lexo.domain.AuditEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class RegisterAuditEntryService implements RegisterAuditEntryUseCase {

    private final SaveAuditEntryPort saveAuditEntryPort;
    private final Clock clock;

    RegisterAuditEntryService(SaveAuditEntryPort saveAuditEntryPort, Clock clock) {
        this.saveAuditEntryPort = saveAuditEntryPort;
        this.clock = clock;
    }

    @Override
    public void register(RegisterAuditEntryCommand command) {
        Instant occurredAt = command.occurredAt() != null ? command.occurredAt() : clock.instant();
        saveAuditEntryPort.save(AuditEntry.register(command.organizationId(), command.userId(), command.userName(),
                command.action(), command.entityType(), command.entityId(), command.description(), occurredAt));
    }
}
