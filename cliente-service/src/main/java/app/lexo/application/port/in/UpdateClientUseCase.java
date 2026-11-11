package app.lexo.application.port.in;

public interface UpdateClientUseCase {

    ClientResponse update(UpdateClientCommand command);
}
