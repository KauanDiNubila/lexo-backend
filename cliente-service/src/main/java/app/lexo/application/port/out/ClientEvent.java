package app.lexo.application.port.out;

import app.lexo.application.port.in.Actor;
import app.lexo.common.event.EventType;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public record ClientEvent(String type, OrganizationId organizationId, ClientId clientId, String description,
                              Actor actor) {

    public static final String CREATED = EventType.CLIENT_CREATED.code();
    public static final String UPDATED = EventType.CLIENT_UPDATED.code();
    public static final String DELETED = EventType.CLIENT_DELETED.code();

    public static ClientEvent created(Client client, Actor actor) {
        return new ClientEvent(CREATED, client.organizationId(), client.id(),
                "Cliente criado: " + client.name(), actor);
    }

    public static ClientEvent updated(Client client, Actor actor) {
        return new ClientEvent(UPDATED, client.organizationId(), client.id(),
                "Cliente atualizado: " + client.name(), actor);
    }

    public static ClientEvent deleted(Client client, Actor actor) {
        return new ClientEvent(DELETED, client.organizationId(), client.id(),
                "Cliente excluído: " + client.name(), actor);
    }
}
