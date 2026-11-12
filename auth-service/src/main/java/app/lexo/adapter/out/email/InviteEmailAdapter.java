package app.lexo.adapter.out.email;

import app.lexo.application.port.out.InviteEmail;
import app.lexo.application.port.out.SendInviteEmailPort;
import app.lexo.common.email.EnfileiradorEmail;
import app.lexo.common.email.TarefaEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class InviteEmailAdapter implements SendInviteEmailPort {

    private final EnfileiradorEmail queue;
    private final String baseUrl;

    InviteEmailAdapter(EnfileiradorEmail queue, @Value("${lexo.base-url}") String baseUrl) {
        this.queue = queue;
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean send(InviteEmail email) {
        String acceptUrl = baseUrl + "/convite/" + email.token();
        return queue.enfileirar(new TarefaEmail("CONVITE", email.to().value(),
                "Convite para " + email.organizationName() + " no Lexo",
                "Olá " + email.inviteeName() + ", você foi convidado para " + email.organizationName()
                        + ". Acesse: " + acceptUrl));
    }
}
