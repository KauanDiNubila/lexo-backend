package app.lexo.application.port.in;

public interface RefreshSessionUseCase {

    SessionResponse refresh(RefreshSessionCommand command);
}
