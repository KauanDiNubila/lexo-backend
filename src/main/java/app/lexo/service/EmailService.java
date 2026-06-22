package app.lexo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * STUB de envio de email (substitui o Resend do projeto original).
 * No nucleo, apenas registra o convite em log. Trocar por um client HTTP da Resend
 * (ou JavaMailSender) quando as credenciais estiverem disponiveis.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendInvite(String toEmail, String orgName, String inviteeName, String acceptUrl) {
        log.info("[email:stub] Convite para {} ({}) entrar em \"{}\": {}",
                inviteeName, toEmail, orgName, acceptUrl);
    }

    public void sendDeadlineReminder(String deadlineTitle, java.time.Instant date) {
        log.info("[email:stub] Lembrete de prazo \"{}\" com vencimento em {}", deadlineTitle, date);
    }
}
