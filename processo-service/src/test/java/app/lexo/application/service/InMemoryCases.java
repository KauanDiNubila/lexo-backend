package app.lexo.application.service;

import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.application.port.out.DeleteCasePort;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.application.port.out.LoadCasesByIdsPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.application.port.out.SaveCasePort;
import app.lexo.domain.Case;
import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryCases implements SaveCasePort, LoadCasePort, CaseExistsPort, DeleteCasePort, LoadClientCasesPort,
        LoadCasesByIdsPort {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    private final Map<String, Case> cases = new LinkedHashMap<>();
    private int sequence;

    @Override
    public Case save(Case value) {
        Case saved = value.id() == null
                ? Case.restore(new CaseId("case-" + (++sequence)), value.organizationId(), value.data(), CREATED)
                : value;
        cases.put(saved.id().value(), saved);
        return saved;
    }

    @Override
    public Optional<Case> byId(OrganizationId organizationId, CaseId caseId) {
        return Optional.ofNullable(cases.get(caseId.value()))
                .filter(value -> value.organizationId().equals(organizationId));
    }

    @Override
    public boolean exists(OrganizationId organizationId, CaseId caseId) {
        return byId(organizationId, caseId).isPresent();
    }

    @Override
    public void delete(OrganizationId organizationId, CaseId caseId) {
        byId(organizationId, caseId).ifPresent(value -> cases.remove(caseId.value()));
    }

    @Override
    public List<Case> ofClient(OrganizationId organizationId, ClientId clientId) {
        return cases.values().stream()
                .filter(value -> value.organizationId().equals(organizationId)
                        && value.data().clientId().equals(clientId))
                .toList();
    }

    @Override
    public List<Case> byIds(Collection<CaseId> caseIds) {
        return cases.values().stream().filter(value -> caseIds.contains(value.id())).toList();
    }

    List<Case> all() {
        return new ArrayList<>(cases.values());
    }
}
