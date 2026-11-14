package app.lexo.application.service;

import app.lexo.application.port.in.ChangeDeadlineStatusCommand;
import app.lexo.application.port.in.ChangeDeadlineStatusUseCase;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.out.LoadDeadlinePort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class ChangeDeadlineStatusService implements ChangeDeadlineStatusUseCase {

    private final LoadDeadlinePort loadDeadlinePort;
    private final SaveDeadlinePort saveDeadlinePort;
    private final Clock clock;

    ChangeDeadlineStatusService(LoadDeadlinePort loadDeadlinePort, SaveDeadlinePort saveDeadlinePort, Clock clock) {
        this.loadDeadlinePort = loadDeadlinePort;
        this.saveDeadlinePort = saveDeadlinePort;
        this.clock = clock;
    }

    @Override
    public DeadlineResponse changeStatus(ChangeDeadlineStatusCommand command) {
        Deadline deadline = loadDeadlinePort.byId(command.organizationId(), command.deadlineId())
                .orElseThrow(DeadlineNotFoundException::new);
        if (command.completed()) {
            deadline.complete();
        } else {
            deadline.reopen();
        }
        return DeadlineResponse.of(saveDeadlinePort.save(deadline), clock.instant());
    }
}
