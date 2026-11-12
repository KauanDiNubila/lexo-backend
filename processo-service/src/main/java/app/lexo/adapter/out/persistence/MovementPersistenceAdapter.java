package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.DeleteMovementPort;
import app.lexo.application.port.out.DeleteMovementsOfCasePort;
import app.lexo.application.port.out.ListMovementsPort;
import app.lexo.application.port.out.LoadMovementsOfCasesPort;
import app.lexo.application.port.out.SaveMovementPort;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
class MovementPersistenceAdapter implements SaveMovementPort, DeleteMovementPort, ListMovementsPort,
        DeleteMovementsOfCasePort, LoadMovementsOfCasesPort {

    private final SpringDataMovementRepository repository;

    MovementPersistenceAdapter(SpringDataMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public CaseMovement save(CaseMovement movement) {
        MovementJpaEntity entity = movement.id() == null
                ? new MovementJpaEntity()
                : repository.findByIdAndOrganizationId(movement.id().value(), movement.organizationId().value())
                        .orElseThrow(() -> new IllegalStateException(
                                "Movement " + movement.id().value() + " does not exist to update"));
        MovementMapper.copyTo(movement, entity);
        return MovementMapper.toDomain(repository.save(entity));
    }

    @Override
    public boolean delete(OrganizationId organizationId, CaseId caseId, MovementId movementId) {
        return repository.deleteByIdAndCaseIdAndOrganizationId(
                movementId.value(), caseId.value(), organizationId.value()) > 0;
    }

    @Override
    public List<CaseMovement> ofCase(OrganizationId organizationId, CaseId caseId) {
        return repository.findByCaseIdAndOrganizationIdOrderByDateDesc(caseId.value(), organizationId.value())
                .stream().map(MovementMapper::toDomain).toList();
    }

    @Override
    public void deleteOfCase(OrganizationId organizationId, CaseId caseId) {
        repository.deleteByCaseIdAndOrganizationId(caseId.value(), organizationId.value());
    }

    @Override
    public List<CaseMovement> ofCases(OrganizationId organizationId, Collection<CaseId> caseIds) {
        return repository.findByCaseIdInAndOrganizationIdOrderByDateDesc(
                        caseIds.stream().map(CaseId::value).toList(), organizationId.value()).stream()
                .map(MovementMapper::toDomain).toList();
    }
}
