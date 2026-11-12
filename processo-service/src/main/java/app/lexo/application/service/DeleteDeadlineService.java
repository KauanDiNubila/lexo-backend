package app.lexo.application.service;

import app.lexo.application.port.in.DeleteDeadlineCommand;
import app.lexo.application.port.in.DeleteDeadlineUseCase;
import app.lexo.application.port.out.DeleteDeadlinePort;
import app.lexo.domain.DeadlineNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteDeadlineService implements DeleteDeadlineUseCase {

    private final DeleteDeadlinePort deleteDeadlinePort;

    DeleteDeadlineService(DeleteDeadlinePort deleteDeadlinePort) {
        this.deleteDeadlinePort = deleteDeadlinePort;
    }

    @Override
    public void delete(DeleteDeadlineCommand command) {
        if (!deleteDeadlinePort.delete(command.organizationId(), command.deadlineId())) {
            throw new DeadlineNotFoundException();
        }
    }
}
