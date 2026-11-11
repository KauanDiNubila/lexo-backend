package app.lexo.application.port.in;

import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

public record RevokePortalLinkCommand(OrganizationId organizationId, ClientId clientId) {
}
