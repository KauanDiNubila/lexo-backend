package app.lexo.adapter.out.mail;

import app.lexo.application.port.out.MailPort;
import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpMailAdapter implements MailPort {

    private final JavaMailSender mailSender;
    private final String sender;

    SmtpMailAdapter(JavaMailSender mailSender, @Value("${lexo.email.from}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    @Override
    public void send(SendEmailCommand command) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(command.recipient());
        message.setSubject(command.subject());
        message.setText(command.body());
        mailSender.send(message);
    }
}
