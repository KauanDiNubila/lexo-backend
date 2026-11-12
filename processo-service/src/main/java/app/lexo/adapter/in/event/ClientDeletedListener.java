package app.lexo.adapter.in.event;

import app.lexo.application.port.in.DeleteClientCasesCommand;
import app.lexo.application.port.in.DeleteClientCasesUseCase;
import app.lexo.common.event.DomainEvent;
import app.lexo.common.event.EventType;
import app.lexo.common.kafka.EventPublisher;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class ClientDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(ClientDeletedListener.class);

    private final DeleteClientCasesUseCase deleteClientCases;

    ClientDeletedListener(DeleteClientCasesUseCase deleteClientCases) {
        this.deleteClientCases = deleteClientCases;
    }

    @KafkaListener(topics = EventPublisher.TOPIC, groupId = "lexo-processo")
    void onEvent(DomainEvent event) {
        if (!event.is(EventType.CLIENT_DELETED)) {
            return;
        }
        int deleted = deleteClientCases.deleteOfClient(new DeleteClientCasesCommand(
                new OrganizationId(event.organizationId()), new ClientId(event.entityId())));
        log.info("[kafka] client {} deleted, {} case(s) removed in cascade", event.entityId(), deleted);
    }
}
