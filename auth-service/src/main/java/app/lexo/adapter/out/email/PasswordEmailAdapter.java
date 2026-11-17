package app.lexo.adapter.out.email;

import app.lexo.application.port.out.PasswordChangedEmail;
import app.lexo.application.port.out.PasswordResetEmail;
import app.lexo.application.port.out.SendPasswordEmailPort;
import app.lexo.common.email.EmailPublisher;
import app.lexo.common.email.EmailTask;
import app.lexo.domain.PasswordReset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PasswordEmailAdapter implements SendPasswordEmailPort {

    private final EmailPublisher queue;
    private final String baseUrl;

    PasswordEmailAdapter(EmailPublisher queue, @Value("${lexo.base-url}") String baseUrl) {
        this.queue = queue;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendResetLink(PasswordResetEmail email) {
        queue.enqueue(new EmailTask("REDEFINICAO_SENHA", email.to().value(),
                "Redefinição de senha no Lexo",
                "Olá " + email.name() + ", recebemos um pedido para redefinir a sua senha. Acesse: "
                        + baseUrl + "/redefinir-senha/" + email.token()
                        + " O link vale por " + PasswordReset.VALIDITY.toHours()
                        + " hora e só pode ser usado uma vez. Se não foi você, ignore este e-mail."));
    }

    @Override
    public void sendChangedNotice(PasswordChangedEmail email) {
        queue.enqueue(new EmailTask("SENHA_ALTERADA", email.to().value(),
                "Sua senha do Lexo foi alterada",
                "Olá " + email.name() + ", a senha da sua conta no Lexo foi alterada. Se não foi você, "
                        + "use \"Esqueci minha senha\" na tela de login e avise o administrador do escritório."));
    }
}
