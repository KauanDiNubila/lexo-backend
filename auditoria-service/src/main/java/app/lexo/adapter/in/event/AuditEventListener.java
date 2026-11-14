package app.lexo.adapter.in.event;

import app.lexo.application.port.in.RegisterAuditEntryCommand;
import app.lexo.application.port.in.RegisterAuditEntryUseCase;
import app.lexo.common.event.DomainEvent;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class AuditEventListener {

    private static final String TOPIC = "lexo.eventos";
    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final RegisterAuditEntryUseCase registerAuditEntry;

    AuditEventListener(RegisterAuditEntryUseCase registerAuditEntry) {
        this.registerAuditEntry = registerAuditEntry;
    }

    @KafkaListener(topics = TOPIC, groupId = "lexo-auditoria")
    void onEvent(DomainEvent event) {
        log.info("[kafka] event received: {} ({})", event.type(), event.entityId());
        registerAuditEntry.register(new RegisterAuditEntryCommand(
                new OrganizationId(event.organizationId()), event.userId(), event.userName(), event.type(),
                event.entityType(), event.entityId(), event.description(), event.occurredAt()));
    }
}
