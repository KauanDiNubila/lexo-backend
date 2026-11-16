package app.lexo.adapter.in.queue;

import app.lexo.application.port.in.SendEmailUseCase;
import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import app.lexo.common.email.RabbitConfig;
import app.lexo.common.email.EmailTask;
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
    void onTask(EmailTask task) {
        log.info("[rabbit] processando e-mail: {} para {}", task.type(), task.recipient());
        sendEmail.send(new SendEmailCommand(task.type(), task.recipient(), task.subject(), task.body()));
    }
}
