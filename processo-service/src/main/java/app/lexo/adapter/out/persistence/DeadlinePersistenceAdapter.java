package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.DeleteDeadlinePort;
import app.lexo.application.port.out.DeleteDeadlinesOfCasePort;
import app.lexo.application.port.out.ListDeadlinesPort;
import app.lexo.application.port.out.LoadDeadlinePort;
import app.lexo.application.port.out.LoadDeadlinesOfCasesPort;
import app.lexo.application.port.out.LoadDeadlinesToRemindPort;
import app.lexo.application.port.out.PendingDeadlinesPort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.OrganizationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
class DeadlinePersistenceAdapter implements SaveDeadlinePort, LoadDeadlinePort, DeleteDeadlinePort,
        ListDeadlinesPort, PendingDeadlinesPort, DeleteDeadlinesOfCasePort, LoadDeadlinesToRemindPort,
        LoadDeadlinesOfCasesPort {

    private final SpringDataDeadlineRepository repository;

    DeadlinePersistenceAdapter(SpringDataDeadlineRepository repository) {
        this.repository = repository;
    }

    @Override
    public Deadline save(Deadline deadline) {
        DeadlineJpaEntity entity = deadline.id() == null
                ? new DeadlineJpaEntity()
                : repository.findByIdAndOrganizationId(deadline.id().value(), deadline.organizationId().value())
                        .orElseThrow(() -> new IllegalStateException(
                                "Deadline " + deadline.id().value() + " does not exist to update"));
        DeadlineMapper.copyTo(deadline, entity);
        return DeadlineMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Deadline> byId(OrganizationId organizationId, DeadlineId deadlineId) {
        return repository.findByIdAndOrganizationId(deadlineId.value(), organizationId.value())
                .map(DeadlineMapper::toDomain);
    }

    @Override
    public void delete(OrganizationId organizationId, DeadlineId deadlineId) {
        repository.deleteByIdAndOrganizationId(deadlineId.value(), organizationId.value());
    }

    @Override
    public PageResult<Deadline> list(OrganizationId organizationId, CaseId caseId, PageParams pageParams) {
        Pageable pageable = PageParamsMapper.toPageRequest(pageParams);
        Page<DeadlineJpaEntity> page = caseId == null
                ? repository.findByOrganizationId(organizationId.value(), pageable)
                : repository.findByOrganizationIdAndCaseId(organizationId.value(), caseId.value(), pageable);
        return PageResult.of(page, DeadlineMapper::toDomain);
    }

    @Override
    public long countBetween(OrganizationId organizationId, Instant from, Instant to) {
        return repository.countByOrganizationIdAndStatusAndDateBetween(
                organizationId.value(), DeadlineStatus.PENDING, from, to);
    }

    @Override
    public long countBefore(OrganizationId organizationId, Instant limit) {
        return repository.countByOrganizationIdAndStatusAndDateBefore(
                organizationId.value(), DeadlineStatus.PENDING, limit);
    }

    @Override
    public List<Deadline> next(OrganizationId organizationId, int limit) {
        return repository.findByOrganizationIdAndStatusOrderByDateAsc(
                        organizationId.value(), DeadlineStatus.PENDING, PageRequest.of(0, limit)).stream()
                .map(DeadlineMapper::toDomain).toList();
    }

    @Override
    public void deleteOfCase(OrganizationId organizationId, CaseId caseId) {
        repository.deleteByCaseIdAndOrganizationId(caseId.value(), organizationId.value());
    }

    @Override
    public List<Deadline> pendingNotNotifiedBetween(Instant from, Instant to) {
        return repository.findByStatusAndNotifiedAtIsNullAndDateBetween(DeadlineStatus.PENDING, from, to).stream()
                .map(DeadlineMapper::toDomain).toList();
    }

    @Override
    public List<Deadline> ofCases(OrganizationId organizationId, Collection<CaseId> caseIds) {
        return repository.findByCaseIdInAndOrganizationIdOrderByDateAsc(
                        caseIds.stream().map(CaseId::value).toList(), organizationId.value()).stream()
                .map(DeadlineMapper::toDomain).toList();
    }
}
