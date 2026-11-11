package app.lexo.common.kafka;

import app.lexo.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnClass(KafkaTemplate.class)
public class EventPublisher {

    public static final String TOPIC = "lexo.eventos";

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, DomainEvent> kafka;

    public EventPublisher(KafkaTemplate<String, DomainEvent> kafka) {
        this.kafka = kafka;
    }

    public void publish(DomainEvent event) {
        try {
            kafka.send(TOPIC, event.organizationId(), event);
            log.info("[kafka] event published: {} ({})", event.type(), event.entityId());
        } catch (Exception e) {
            log.error("[kafka] failed to publish event {}, continuing without blocking", event.type(), e);
        }
    }
}
