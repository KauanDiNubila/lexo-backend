package app.lexo.evento;

import app.lexo.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos de dominio do Kafka (publicados por outros servicos) e grava no
 * log de auditoria. E o coracao event-driven deste servico: nenhum produtor sabe que a
 * auditoria existe — ela apenas reage aos fatos que aconteceram.
 */
@Component
public class ConsumidorEventos {

    private static final String TOPICO = "lexo.eventos";

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final AuditoriaService auditoria;

    public ConsumidorEventos(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @KafkaListener(topics = TOPICO, groupId = "lexo-auditoria")
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
