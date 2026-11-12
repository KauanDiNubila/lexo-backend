package app.lexo.application.service;

import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.ScheduleDeadlineCommand;
import app.lexo.application.port.in.ScheduleDeadlineUseCase;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.domain.Deadline;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class ScheduleDeadlineService implements ScheduleDeadlineUseCase {

    private final CaseGuard guard;
    private final SaveDeadlinePort saveDeadlinePort;
    private final Clock clock;

    ScheduleDeadlineService(CaseGuard guard, SaveDeadlinePort saveDeadlinePort, Clock clock) {
        this.guard = guard;
        this.saveDeadlinePort = saveDeadlinePort;
        this.clock = clock;
    }

    @Override
    public DeadlineResponse schedule(ScheduleDeadlineCommand command) {
        guard.requireExists(command.organizationId(), command.data().caseId());
        Deadline saved = saveDeadlinePort.save(Deadline.schedule(command.organizationId(), command.data()));
        return DeadlineResponse.of(saved, clock.instant());
    }
}
