package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;

public record RenameOrganizationCommand(OrganizationId organizationId, String name) {

    public RenameOrganizationCommand {
        Input.requireMinLength(name, 2, "Nome do escritório muito curto");
    }
}
