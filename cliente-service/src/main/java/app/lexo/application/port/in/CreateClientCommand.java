package app.lexo.application.port.in;

import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;

public record CreateClientCommand(OrganizationId organizationId, Actor actor, ClientData data) {

    public CreateClientCommand {
        ClientName.requireValid(data.name());
    }
}
