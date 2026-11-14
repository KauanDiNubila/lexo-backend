package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public record DeleteClientCommand(OrganizationId organizationId, ClientId clientId, Actor actor) {
}
