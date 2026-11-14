package app.lexo.adapter.out.cases;

import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class CaseExistsAdapter implements CaseExistsPort {

    private static final Logger log = LoggerFactory.getLogger(CaseExistsAdapter.class);

    private final CaseServiceClient client;

    CaseExistsAdapter(CaseServiceClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "processo-service", fallbackMethod = "existsFallback")
    public boolean exists(OrganizationId organizationId, CaseId caseId) {
        return client.caseExists(caseId.value(), organizationId.value()).exists();
    }

    @SuppressWarnings("unused")
    private boolean existsFallback(OrganizationId organizationId, CaseId caseId, Throwable cause) {
        log.warn("processo-service unavailable (circuit breaker): {}", cause.toString());
        throw new ServiceUnavailableException(
                "Serviço de processos temporariamente indisponível. Tente novamente em instantes.");
    }
}
