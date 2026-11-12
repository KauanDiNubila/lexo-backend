package app.lexo.adapter.out.client;

import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ClientExistsAdapter implements ClientExistsPort {

    private static final Logger log = LoggerFactory.getLogger(ClientExistsAdapter.class);

    private final ClientServiceClient client;

    ClientExistsAdapter(ClientServiceClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "cliente-service", fallbackMethod = "existsFallback")
    public boolean exists(OrganizationId organizationId, ClientId clientId) {
        return client.clientExists(clientId.value(), organizationId.value()).exists();
    }

    @SuppressWarnings("unused")
    private boolean existsFallback(OrganizationId organizationId, ClientId clientId, Throwable cause) {
        log.warn("cliente-service unavailable (circuit breaker): {}", cause.toString());
        throw new ServiceUnavailableException(
                "Servi\u00e7o de clientes temporariamente indispon\u00edvel. Tente novamente em instantes.");
    }
}
