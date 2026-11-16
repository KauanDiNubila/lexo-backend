package app.lexo.adapter.out.mail;

import app.lexo.application.port.out.MailPort;
import app.lexo.domain.Email;
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
    public void send(Email email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email.recipient());
        message.setSubject(email.subject());
        message.setText(email.body());
        mailSender.send(message);
    }
}
