package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public record RemoveUserCommand(OrganizationId organizationId, Actor actor, UserId targetId) {
}
