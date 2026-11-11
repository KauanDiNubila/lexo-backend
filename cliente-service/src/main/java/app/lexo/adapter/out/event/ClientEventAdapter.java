package app.lexo.adapter.out.event;

import app.lexo.application.port.out.ClientEvent;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.common.evento.EventoDominio;
import app.lexo.common.kafka.PublicadorEventos;
import org.springframework.stereotype.Component;

@Component
class ClientEventAdapter implements PublishEventPort {

    private static final String ENTITY_TYPE = "CLIENTE";

    private final PublicadorEventos publisher;

    ClientEventAdapter(PublicadorEventos publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(ClientEvent event) {
        publisher.publicar(EventoDominio.de(event.type(), event.organizationId().value(), ENTITY_TYPE,
                event.clientId().value(), event.description(), event.actor().userId(), event.actor().name()));
    }
}
