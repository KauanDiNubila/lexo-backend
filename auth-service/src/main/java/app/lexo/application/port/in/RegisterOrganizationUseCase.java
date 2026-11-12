package app.lexo.application.port.in;

public interface RegisterOrganizationUseCase {

    SessionResponse register(RegisterOrganizationCommand command);
}
