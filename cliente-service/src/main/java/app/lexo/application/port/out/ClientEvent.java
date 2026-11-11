package app.lexo.application.port.out;

import app.lexo.application.port.in.Actor;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public record ClientEvent(String type, OrganizationId organizationId, ClientId clientId, String description,
                              Actor actor) {

    public static final String CREATED = "CLIENTE_CRIADO";
    public static final String DELETED = "CLIENTE_EXCLUIDO";

    public static ClientEvent created(Client client, Actor actor) {
        return new ClientEvent(CREATED, client.organizationId(), client.id(),
                "Cliente criado: " + client.name(), actor);
    }

    public static ClientEvent deleted(Client client, Actor actor) {
        return new ClientEvent(DELETED, client.organizationId(), client.id(),
                "Cliente excluído: " + client.name(), actor);
    }
}
