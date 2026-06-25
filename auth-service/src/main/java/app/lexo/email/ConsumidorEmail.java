package app.lexo.email;

import app.lexo.config.RabbitConfig;
import app.lexo.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome a fila de e-mails e dispara o envio de fato. Se lancar excecao, o RabbitMQ
 * reentrega ate o limite de retries; esgotado, a mensagem vai para a dead-letter queue.
 */
@Component
public class ConsumidorEmail {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEmail.class);

    private final EmailService email;

    public ConsumidorEmail(EmailService email) {
        this.email = email;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void processar(TarefaEmail tarefa) {
        log.info("[rabbit] processando e-mail: {} para {}", tarefa.tipo(), tarefa.destinatario());
        email.enviar(tarefa);
    }
}
