package app.lexo.adapter.out.persistence;

import app.lexo.application.port.in.CaseOption;
import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.application.port.out.CountCasesByStatusPort;
import app.lexo.application.port.out.DeleteCasePort;
import app.lexo.application.port.out.ListCaseOptionsPort;
import app.lexo.application.port.out.ListCasesPort;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.application.port.out.LoadCasesByIdsPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.application.port.out.SaveCasePort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Case;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
class CasePersistenceAdapter implements SaveCasePort, LoadCasePort, CaseExistsPort, DeleteCasePort, ListCasesPort,
        ListCaseOptionsPort, CountCasesByStatusPort, LoadClientCasesPort, LoadCasesByIdsPort {

    private static final String OPTIONS_CACHE = "processos-opcoes-v2";

    private final SpringDataCaseRepository repository;

    CasePersistenceAdapter(SpringDataCaseRepository repository) {
        this.repository = repository;
    }

    @Override
    @CacheEvict(value = OPTIONS_CACHE, key = "#domainCase.organizationId().value()")
    public Case save(Case domainCase) {
        CaseJpaEntity entity = domainCase.id() == null
                ? new CaseJpaEntity()
                : repository.findByIdAndOrganizationId(domainCase.id().value(), domainCase.organizationId().value())
                        .orElseThrow(() -> new IllegalStateException(
                                "Case " + domainCase.id().value() + " does not exist to update"));
        CaseMapper.copyTo(domainCase, entity);
        return CaseMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Case> byId(OrganizationId organizationId, CaseId caseId) {
        return repository.findByIdAndOrganizationId(caseId.value(), organizationId.value())
                .map(CaseMapper::toDomain);
    }

    @Override
    public boolean exists(OrganizationId organizationId, CaseId caseId) {
        return repository.existsByIdAndOrganizationId(caseId.value(), organizationId.value());
    }

    @Override
    @CacheEvict(value = OPTIONS_CACHE, key = "#organizationId.value()")
    public void delete(OrganizationId organizationId, CaseId caseId) {
        repository.deleteByIdAndOrganizationId(caseId.value(), organizationId.value());
    }

    @Override
    public PageResult<Case> list(OrganizationId organizationId, CaseFilter filter, PageParams pageParams) {
        return PageResult.of(repository.findAll(CaseSpecification.of(organizationId, filter),
                PageParamsMapper.toPageRequest(pageParams)), CaseMapper::toDomain);
    }

    @Override
    @Cacheable(value = OPTIONS_CACHE, key = "#organizationId.value()")
    public List<CaseOption> options(OrganizationId organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId.value()).stream()
                .map(entity -> new CaseOption(entity.getId(), entity.number, entity.clientId))
                .toList();
    }

    @Override
    public Map<CaseStatus, Long> countByStatus(OrganizationId organizationId) {
        Map<CaseStatus, Long> counts = new LinkedHashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            counts.put(status, repository.countByOrganizationIdAndStatus(organizationId.value(), status));
        }
        return counts;
    }

    @Override
    public List<Case> ofClient(OrganizationId organizationId, ClientId clientId) {
        return repository.findByClientIdAndOrganizationIdOrderByCreatedAtDesc(clientId.value(), organizationId.value())
                .stream().map(CaseMapper::toDomain).toList();
    }

    @Override
    public List<Case> byIds(Collection<CaseId> caseIds) {
        return repository.findAllById(caseIds.stream().map(CaseId::value).toList()).stream()
                .map(CaseMapper::toDomain).toList();
    }
}
