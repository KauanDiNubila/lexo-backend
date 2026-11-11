package app.lexo.application.service;

import app.lexo.application.port.out.EventoDeCliente;
import app.lexo.application.port.out.PublicarEventoPort;

import java.util.ArrayList;
import java.util.List;

class EventosEmMemoria implements PublicarEventoPort {

    final List<EventoDeCliente> publicados = new ArrayList<>();

    @Override
    public void publicar(EventoDeCliente evento) {
        publicados.add(evento);
    }
}
