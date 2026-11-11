package app.lexo.adapter.in.event;

import app.lexo.application.port.in.DeleteClientInvoicesCommand;
import app.lexo.application.port.in.DeleteClientInvoicesUseCase;
import app.lexo.application.port.in.DetachCaseCommand;
import app.lexo.application.port.in.DetachCaseUseCase;
import app.lexo.common.evento.EventoDominio;
import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class EntityDeletedListener {

    private static final String TOPIC = "lexo.eventos";
    private static final Logger log = LoggerFactory.getLogger(EntityDeletedListener.class);

    private final DeleteClientInvoicesUseCase deleteClientInvoices;
    private final DetachCaseUseCase detachCase;

    EntityDeletedListener(DeleteClientInvoicesUseCase deleteClientInvoices, DetachCaseUseCase detachCase) {
        this.deleteClientInvoices = deleteClientInvoices;
        this.detachCase = detachCase;
    }

    @KafkaListener(topics = TOPIC, groupId = "lexo-financeiro")
    void onEvent(EventoDominio event) {
        switch (event.tipo()) {
            case "CLIENTE_EXCLUIDO" -> {
                int deleted = deleteClientInvoices.deleteOfClient(new DeleteClientInvoicesCommand(
                        new OrganizationId(event.organizationId()), new ClientId(event.entityId())));
                log.info("[kafka] client {} deleted, {} invoice(s) removed in cascade", event.entityId(), deleted);
            }
            case "PROCESSO_EXCLUIDO" -> {
                int detached = detachCase.detach(new DetachCaseCommand(
                        new OrganizationId(event.organizationId()), new CaseId(event.entityId())));
                log.info("[kafka] case {} deleted, {} invoice(s) detached", event.entityId(), detached);
            }
            default -> {
            }
        }
    }
}
