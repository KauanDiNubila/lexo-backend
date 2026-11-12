package app.lexo.application.port.in;

public interface LoginUseCase {

    SessionResponse login(LoginCommand command);
}
