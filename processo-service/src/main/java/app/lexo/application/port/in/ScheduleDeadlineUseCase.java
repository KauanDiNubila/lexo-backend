package app.lexo.application.port.in;

public interface ScheduleDeadlineUseCase {

    DeadlineResponse schedule(ScheduleDeadlineCommand command);
}
