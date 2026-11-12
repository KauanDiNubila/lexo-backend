package app.lexo.application.service;

import app.lexo.application.port.in.MovementResponse;
import app.lexo.application.port.in.RecordMovementCommand;
import app.lexo.application.port.in.RecordMovementUseCase;
import app.lexo.application.port.out.SaveMovementPort;
import app.lexo.domain.CaseMovement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RecordMovementService implements RecordMovementUseCase {

    private final CaseGuard guard;
    private final SaveMovementPort saveMovementPort;

    RecordMovementService(CaseGuard guard, SaveMovementPort saveMovementPort) {
        this.guard = guard;
        this.saveMovementPort = saveMovementPort;
    }

    @Override
    public MovementResponse record(RecordMovementCommand command) {
        guard.requireExists(command.organizationId(), command.caseId());
        CaseMovement saved = saveMovementPort.save(
                CaseMovement.record(command.organizationId(), command.caseId(), command.data()));
        return MovementResponse.of(saved);
    }
}
