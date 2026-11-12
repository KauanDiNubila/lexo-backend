package app.lexo.application.port.in;

public interface ChangeDeadlineStatusUseCase {

    DeadlineResponse changeStatus(ChangeDeadlineStatusCommand command);
}
