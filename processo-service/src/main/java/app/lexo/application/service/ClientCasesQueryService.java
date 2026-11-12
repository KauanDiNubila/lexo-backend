package app.lexo.application.service;

import app.lexo.application.port.in.CaseOverview;
import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.in.ListClientCasesQuery;
import app.lexo.application.port.in.MovementResponse;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.application.port.out.LoadDeadlinesOfCasesPort;
import app.lexo.application.port.out.LoadMovementsOfCasesPort;
import app.lexo.domain.Case;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
class ClientCasesQueryService implements ListClientCasesQuery {

    private final LoadClientCasesPort loadClientCasesPort;
    private final LoadDeadlinesOfCasesPort loadDeadlinesPort;
    private final LoadMovementsOfCasesPort loadMovementsPort;
    private final Clock clock;

    ClientCasesQueryService(LoadClientCasesPort loadClientCasesPort, LoadDeadlinesOfCasesPort loadDeadlinesPort,
                            LoadMovementsOfCasesPort loadMovementsPort, Clock clock) {
        this.loadClientCasesPort = loadClientCasesPort;
        this.loadDeadlinesPort = loadDeadlinesPort;
        this.loadMovementsPort = loadMovementsPort;
        this.clock = clock;
    }

    @Override
    public List<CaseOverview> list(OrganizationId organizationId, ClientId clientId) {
        List<Case> cases = loadClientCasesPort.ofClient(organizationId, clientId);
        if (cases.isEmpty()) {
            return List.of();
        }
        List<CaseId> ids = cases.stream().map(Case::id).toList();
        Instant now = clock.instant();
        Map<CaseId, List<DeadlineResponse>> deadlines = loadDeadlinesPort.ofCases(organizationId, ids).stream()
                .collect(Collectors.groupingBy(deadline -> deadline.data().caseId(),
                        Collectors.mapping(deadline -> DeadlineResponse.of(deadline, now), Collectors.toList())));
        Map<CaseId, List<MovementResponse>> movements = loadMovementsPort.ofCases(organizationId, ids).stream()
                .collect(Collectors.groupingBy(CaseMovement::caseId,
                        Collectors.mapping(MovementResponse::of, Collectors.toList())));
        return cases.stream()
                .map(value -> new CaseOverview(CaseResponse.of(value),
                        deadlines.getOrDefault(value.id(), List.of()),
                        movements.getOrDefault(value.id(), List.of())))
                .toList();
    }
}
