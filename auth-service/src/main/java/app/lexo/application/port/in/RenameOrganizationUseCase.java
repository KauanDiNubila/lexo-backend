package app.lexo.application.port.in;

public interface RenameOrganizationUseCase {

    OrganizationInfo rename(RenameOrganizationCommand command);
}
