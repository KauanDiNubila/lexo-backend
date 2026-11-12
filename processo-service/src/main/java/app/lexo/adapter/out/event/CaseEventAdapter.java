package app.lexo.adapter.out.event;

import app.lexo.application.port.out.CaseEvent;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.common.event.DomainEvent;
import app.lexo.common.kafka.EventPublisher;
import org.springframework.stereotype.Component;

@Component
class CaseEventAdapter implements PublishEventPort {

    private static final String ENTITY_TYPE = "PROCESSO";

    private final EventPublisher publisher;

    CaseEventAdapter(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(CaseEvent event) {
        publisher.publish(DomainEvent.of(event.type(), event.organizationId().value(), ENTITY_TYPE,
                event.caseId().value(), event.description(), event.actor().userId(), event.actor().name()));
    }
}
