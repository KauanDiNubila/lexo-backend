package app.lexo.application.port.in;

public interface RecordMovementUseCase {

    MovementResponse record(RecordMovementCommand command);
}
