package app.lexo.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PublicadorEventos {

    public static final String TOPICO = "lexo.eventos";

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final KafkaTemplate<String, EventoDominio> kafka;

    public PublicadorEventos(KafkaTemplate<String, EventoDominio> kafka) {
        this.kafka = kafka;
    }

    public void publicar(EventoDominio evento) {
        try {
            kafka.send(TOPICO, evento.organizationId(), evento);
            log.info("[kafka] evento publicado: {} ({})", evento.tipo(), evento.entityId());
        } catch (Exception e) {
            log.error("[kafka] falha ao publicar evento {} — seguindo sem bloquear", evento.tipo(), e);
        }
    }
}
