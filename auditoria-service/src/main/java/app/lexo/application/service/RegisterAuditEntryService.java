package app.lexo.application.service;

import app.lexo.application.port.in.RegisterAuditEntryCommand;
import app.lexo.application.port.in.RegisterAuditEntryUseCase;
import app.lexo.application.port.out.SaveAuditEntryPort;
import app.lexo.domain.AuditEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RegisterAuditEntryService implements RegisterAuditEntryUseCase {

    private final SaveAuditEntryPort saveAuditEntryPort;

    RegisterAuditEntryService(SaveAuditEntryPort saveAuditEntryPort) {
        this.saveAuditEntryPort = saveAuditEntryPort;
    }

    @Override
    public void register(RegisterAuditEntryCommand command) {
        saveAuditEntryPort.save(AuditEntry.register(command.organizationId(), command.userId(), command.userName(),
                command.action(), command.entityType(), command.entityId(), command.description()));
    }
}
