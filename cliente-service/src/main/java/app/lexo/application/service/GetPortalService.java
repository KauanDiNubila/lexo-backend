package app.lexo.application.service;

import app.lexo.application.port.in.GetPortalQuery;
import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.in.PortalSummary;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.LoadClientFeesPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.common.error.ResourceNotFoundException;
import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.domain.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
class GetPortalService implements GetPortalQuery {

    private static final Logger log = LoggerFactory.getLogger(GetPortalService.class);
    private static final Set<String> OUTSTANDING_STATUSES = Set.of("PENDENTE", "ATRASADO");

    private final LoadClientPort loadClientPort;
    private final LoadClientCasesPort loadCasesPort;
    private final LoadClientFeesPort loadFeesPort;
    private final Clock clock;

    GetPortalService(LoadClientPort loadClientPort,
                           LoadClientCasesPort loadCasesPort,
                           LoadClientFeesPort loadFeesPort, Clock clock) {
        this.loadClientPort = loadClientPort;
        this.loadCasesPort = loadCasesPort;
        this.loadFeesPort = loadFeesPort;
        this.clock = clock;
    }

    @Override
    public PortalResponse get(String token) {
        Instant now = clock.instant();
        Client client = loadClientPort.byPortalLink(token)
                .filter(c -> c.portalActiveAt(now))
                .orElseThrow(() -> new ResourceNotFoundException("Link inválido ou expirado"));

        List<PortalCase> cases = orEmpty(
                () -> loadCasesPort.load(client.organizationId(), client.id()), "cases");
        List<PortalFee> fees = orEmpty(
                () -> loadFeesPort.load(client.organizationId(), client.id()), "fees");

        BigDecimal outstanding = fees.stream()
                .filter(h -> OUTSTANDING_STATUSES.contains(h.status()))
                .map(PortalFee::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortalResponse(client.name(), cases, fees,
                new PortalSummary(cases.size(), outstanding));
    }

    private <T> List<T> orEmpty(Supplier<List<T>> call, String name) {
        try {
            return call.get();
        } catch (Exception e) {
            log.warn("Portal: failed to load {} ({}). Returning empty.", name, e.toString());
            return List.of();
        }
    }
}
