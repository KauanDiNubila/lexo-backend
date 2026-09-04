package app.lexo.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnClass(RabbitTemplate.class)
public class EnfileiradorEmail {

    private static final Logger log = LoggerFactory.getLogger(EnfileiradorEmail.class);

    private final RabbitTemplate rabbit;

    public EnfileiradorEmail(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void enfileirar(TarefaEmail tarefa) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, tarefa);
            log.info("[rabbit] e-mail enfileirado: {} para {}", tarefa.tipo(), tarefa.destinatario());
        } catch (Exception e) {
            log.error("[rabbit] falha ao enfileirar e-mail {} — seguindo sem bloquear", tarefa.tipo(), e);
        }
    }
}
