package app.lexo.adapter.out.event;

import app.lexo.application.port.out.EventoDeCliente;
import app.lexo.application.port.out.PublicarEventoPort;
import app.lexo.common.evento.EventoDominio;
import app.lexo.common.kafka.PublicadorEventos;
import org.springframework.stereotype.Component;

@Component
class ClienteEventoAdapter implements PublicarEventoPort {

    private static final String TIPO_DA_ENTIDADE = "CLIENTE";

    private final PublicadorEventos publicador;

    ClienteEventoAdapter(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(EventoDeCliente evento) {
        publicador.publicar(EventoDominio.de(evento.tipo(), evento.escritorioId().valor(), TIPO_DA_ENTIDADE,
                evento.clienteId().valor(), evento.descricao(), evento.autor().usuarioId(), evento.autor().nome()));
    }
}
