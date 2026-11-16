package app.lexo.adapter.in.queue;

import app.lexo.application.port.in.SendEmailUseCase;
import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import app.lexo.common.email.RabbitConfig;
import app.lexo.common.email.TarefaEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class EmailQueueListener {

    private static final Logger log = LoggerFactory.getLogger(EmailQueueListener.class);

    private final SendEmailUseCase sendEmail;

    EmailQueueListener(SendEmailUseCase sendEmail) {
        this.sendEmail = sendEmail;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    void onTask(TarefaEmail task) {
        log.info("[rabbit] processando e-mail: {} para {}", task.tipo(), task.destinatario());
        sendEmail.send(new SendEmailCommand(task.tipo(), task.destinatario(), task.assunto(), task.corpo()));
    }
}
