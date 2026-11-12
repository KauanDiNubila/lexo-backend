package app.lexo.application.service;

import app.lexo.application.port.in.DeleteMovementCommand;
import app.lexo.application.port.in.DeleteMovementUseCase;
import app.lexo.application.port.out.DeleteMovementPort;
import app.lexo.domain.MovementNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteMovementService implements DeleteMovementUseCase {

    private final CaseGuard guard;
    private final DeleteMovementPort deleteMovementPort;

    DeleteMovementService(CaseGuard guard, DeleteMovementPort deleteMovementPort) {
        this.guard = guard;
        this.deleteMovementPort = deleteMovementPort;
    }

    @Override
    public void delete(DeleteMovementCommand command) {
        guard.requireExists(command.organizationId(), command.caseId());
        if (!deleteMovementPort.delete(command.organizationId(), command.movementId())) {
            throw new MovementNotFoundException();
        }
    }
}
