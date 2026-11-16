package app.lexo.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnClass(RabbitTemplate.class)
public class EmailPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailPublisher.class);

    private final RabbitTemplate rabbit;

    public EmailPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public boolean enqueue(EmailTask task) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, task);
            log.info("[rabbit] e-mail enfileirado: {} para {}", task.type(), task.recipient());
            return true;
        } catch (Exception e) {
            log.error("[rabbit] falha ao enfileirar e-mail {} — seguindo sem bloquear", task.type(), e);
            return false;
        }
    }
}
