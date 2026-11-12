package app.lexo.application.port.in;

public interface UpdateDeadlineUseCase {

    DeadlineResponse update(UpdateDeadlineCommand command);
}
