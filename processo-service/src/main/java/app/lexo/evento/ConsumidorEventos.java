package app.lexo.evento;

import app.lexo.common.event.DomainEvent;
import app.lexo.common.event.EventType;
import app.lexo.common.kafka.EventPublisher;
import app.lexo.service.ProcessoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorEventos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final ProcessoService processos;

    public ConsumidorEventos(ProcessoService processos) {
        this.processos = processos;
    }

    @KafkaListener(topics = EventPublisher.TOPIC, groupId = "lexo-processo")
    public void consumir(DomainEvent evento) {
        if (!evento.is(EventType.CLIENT_DELETED)) {
            return;
        }
        int apagados = processos.excluirPorCliente(evento.organizationId(), evento.entityId());
        log.info("[kafka] cliente {} excluido — {} processo(s) removido(s) em cascata",
                evento.entityId(), apagados);
    }
}
