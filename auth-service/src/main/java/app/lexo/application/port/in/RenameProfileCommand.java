package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public record RenameProfileCommand(OrganizationId organizationId, UserId userId, String name) {

    public RenameProfileCommand {
        Input.requireMinLength(name, 2, "Nome muito curto");
    }
}
