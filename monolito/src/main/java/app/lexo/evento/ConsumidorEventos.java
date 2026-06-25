package app.lexo.evento;

import app.lexo.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos de dominio do Kafka e grava no log de auditoria.
 * Hoje roda no mesmo processo; na Fase 2 vira o auditoria-service separado — o codigo
 * praticamente nao muda, so deixa de ser "interno" e passa a cruzar a rede.
 */
@Component
public class ConsumidorEventos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final AuditoriaService auditoria;

    public ConsumidorEventos(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @KafkaListener(topics = PublicadorEventos.TOPICO, groupId = "lexo-auditoria")
    public void consumir(EventoDominio evento) {
        log.info("[kafka] evento recebido: {} ({})", evento.tipo(), evento.entityId());
        auditoria.registrar(
                evento.organizationId(),
                evento.userId(),
                evento.userName(),
                evento.tipo(),
                evento.entityType(),
                evento.entityId(),
                evento.descricao());
    }
}
