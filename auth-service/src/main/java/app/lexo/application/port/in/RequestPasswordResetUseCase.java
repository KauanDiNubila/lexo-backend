package app.lexo.application.port.in;

public interface RequestPasswordResetUseCase {

    void request(RequestPasswordResetCommand command);
}
