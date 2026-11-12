package app.lexo.application.port.in;

import app.lexo.domain.InviteId;
import app.lexo.domain.OrganizationId;

public record RevokeInviteCommand(OrganizationId organizationId, Actor actor, InviteId inviteId) {
}
