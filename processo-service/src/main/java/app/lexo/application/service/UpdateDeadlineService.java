package app.lexo.application.service;

import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.UpdateDeadlineCommand;
import app.lexo.application.port.in.UpdateDeadlineUseCase;
import app.lexo.application.port.out.LoadDeadlinePort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class UpdateDeadlineService implements UpdateDeadlineUseCase {

    private final CaseGuard guard;
    private final LoadDeadlinePort loadDeadlinePort;
    private final SaveDeadlinePort saveDeadlinePort;
    private final Clock clock;

    UpdateDeadlineService(CaseGuard guard, LoadDeadlinePort loadDeadlinePort, SaveDeadlinePort saveDeadlinePort,
                          Clock clock) {
        this.guard = guard;
        this.loadDeadlinePort = loadDeadlinePort;
        this.saveDeadlinePort = saveDeadlinePort;
        this.clock = clock;
    }

    @Override
    public DeadlineResponse update(UpdateDeadlineCommand command) {
        guard.requireExists(command.organizationId(), command.data().caseId());
        Deadline deadline = loadDeadlinePort.byId(command.organizationId(), command.deadlineId())
                .orElseThrow(DeadlineNotFoundException::new);
        deadline.update(command.data());
        return DeadlineResponse.of(saveDeadlinePort.save(deadline), clock.instant());
    }
}
