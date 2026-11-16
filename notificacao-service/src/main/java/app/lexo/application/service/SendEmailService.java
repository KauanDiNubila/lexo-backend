package app.lexo.application.service;

import app.lexo.application.port.in.SendEmailUseCase;
import app.lexo.application.port.out.MailPort;
import app.lexo.domain.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class SendEmailService implements SendEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendEmailService.class);

    private final MailPort mail;

    SendEmailService(MailPort mail) {
        this.mail = mail;
    }

    @Override
    public void send(SendEmailCommand command) {
        mail.send(new Email(command.recipient(), command.subject(), command.body()));
        log.info("[email] enviado '{}' para {}", command.type(), command.recipient());
    }
}
