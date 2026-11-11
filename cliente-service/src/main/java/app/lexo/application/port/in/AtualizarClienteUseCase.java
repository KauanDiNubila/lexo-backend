package app.lexo.application.port.in;

public interface AtualizarClienteUseCase {

    ClienteResponse atualizar(AtualizarClienteCommand command);
}
