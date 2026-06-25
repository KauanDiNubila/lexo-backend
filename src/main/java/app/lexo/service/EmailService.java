package app.lexo.service;

import app.lexo.email.TarefaEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * STUB de envio de e-mail (substitui o Resend do projeto original). E chamado pelo
 * consumidor da fila do RabbitMQ — por isso so registra em log. Trocar por um client
 * HTTP da Resend (ou JavaMailSender) quando as credenciais estiverem disponiveis.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void enviar(TarefaEmail tarefa) {
        log.info("[email:stub] enviando '{}' para {} — assunto: {}",
                tarefa.tipo(), tarefa.destinatario(), tarefa.assunto());
    }
}
