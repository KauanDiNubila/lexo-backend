package app.lexo.application.port.out;

import app.lexo.domain.AuditEntry;

public interface SaveAuditEntryPort {

    AuditEntry save(AuditEntry entry);
}
