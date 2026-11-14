package app.lexo.application.service;

import app.lexo.application.port.out.DeleteMovementPort;
import app.lexo.application.port.out.DeleteMovementsOfCasePort;
import app.lexo.application.port.out.SaveMovementPort;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class InMemoryMovements implements SaveMovementPort, DeleteMovementPort, DeleteMovementsOfCasePort {

    private final Map<String, CaseMovement> movements = new LinkedHashMap<>();
    private int sequence;

    @Override
    public CaseMovement save(CaseMovement movement) {
        CaseMovement saved = movement.id() == null
                ? CaseMovement.restore(new MovementId("movement-" + (++sequence)), movement.organizationId(),
                        movement.caseId(), movement.data(), Instant.parse("2026-01-01T00:00:00Z"))
                : movement;
        movements.put(saved.id().value(), saved);
        return saved;
    }

    @Override
    public boolean delete(OrganizationId organizationId, CaseId caseId, MovementId movementId) {
        CaseMovement movement = movements.get(movementId.value());
        if (movement == null || !movement.organizationId().equals(organizationId)
                || !movement.caseId().equals(caseId)) {
            return false;
        }
        movements.remove(movementId.value());
        return true;
    }

    @Override
    public void deleteOfCase(OrganizationId organizationId, CaseId caseId) {
        movements.values().removeIf(movement -> movement.organizationId().equals(organizationId)
                && movement.caseId().equals(caseId));
    }

    List<CaseMovement> all() {
        return new ArrayList<>(movements.values());
    }
}
