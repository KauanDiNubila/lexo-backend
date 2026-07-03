package app.lexo.service;

import app.lexo.email.TarefaEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailService(JavaMailSender mailSender, @Value("${lexo.email.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviar(TarefaEmail tarefa) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(tarefa.destinatario());
        mensagem.setSubject(tarefa.assunto());
        mensagem.setText(tarefa.corpo());

        mailSender.send(mensagem);
        log.info("[email] enviado '{}' para {}", tarefa.tipo(), tarefa.destinatario());
    }
}
