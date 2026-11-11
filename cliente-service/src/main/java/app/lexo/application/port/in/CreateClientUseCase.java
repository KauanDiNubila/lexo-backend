package app.lexo.application.port.in;

public interface CreateClientUseCase {

    ClientResponse create(CreateClientCommand command);
}
