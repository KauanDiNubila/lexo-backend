package app.lexo.adapter.out.event;

import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.common.event.DomainEvent;
import app.lexo.common.kafka.EventPublisher;
import org.springframework.stereotype.Component;

@Component
class AccountEventAdapter implements PublishEventPort {

    private final EventPublisher publisher;

    AccountEventAdapter(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(AccountEvent event) {
        publisher.publish(DomainEvent.of(event.type().code(), event.organizationId().value(), event.entityType(),
                event.entityId(), event.description(), event.actor().userId().value(), event.actor().name()));
    }
}
