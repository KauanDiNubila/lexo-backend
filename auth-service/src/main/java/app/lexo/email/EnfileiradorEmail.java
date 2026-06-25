package app.lexo.email;

import app.lexo.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Coloca tarefas de e-mail na fila do RabbitMQ. A operacao de origem (ex.: convidar
 * usuario) retorna imediatamente, sem esperar o envio — o consumidor cuida disso depois.
 * Falha ao enfileirar nao quebra a requisicao principal.
 */
@Service
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
