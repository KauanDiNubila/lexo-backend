package app.lexo.evento;

import app.lexo.common.event.DomainEvent;
import app.lexo.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorEventos {

    private static final String TOPICO = "lexo.eventos";

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final AuditoriaService auditoria;

    public ConsumidorEventos(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @KafkaListener(topics = TOPICO, groupId = "lexo-auditoria")
    public void consumir(DomainEvent evento) {
        log.info("[kafka] evento recebido: {} ({})", evento.type(), evento.entityId());
        auditoria.registrar(
                evento.organizationId(),
                evento.userId(),
                evento.userName(),
                evento.type(),
                evento.entityType(),
                evento.entityId(),
                evento.description());
    }
}
