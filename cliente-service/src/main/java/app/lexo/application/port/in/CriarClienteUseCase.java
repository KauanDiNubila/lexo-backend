package app.lexo.application.port.in;

public interface CriarClienteUseCase {

    ClienteResponse criar(CriarClienteCommand command);
}
