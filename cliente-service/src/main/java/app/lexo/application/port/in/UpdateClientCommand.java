package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;

public record UpdateClientCommand(OrganizationId organizationId, ClientId clientId, Actor actor, ClientData data) {

    public UpdateClientCommand {
        ClientName.requireValid(data.name());
    }
}
