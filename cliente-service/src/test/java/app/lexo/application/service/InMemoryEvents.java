package app.lexo.application.service;

import app.lexo.application.port.out.ClientEvent;
import app.lexo.application.port.out.PublishEventPort;

import java.util.ArrayList;
import java.util.List;

class InMemoryEvents implements PublishEventPort {

    final List<ClientEvent> published = new ArrayList<>();

    @Override
    public void publish(ClientEvent event) {
        published.add(event);
    }
}
