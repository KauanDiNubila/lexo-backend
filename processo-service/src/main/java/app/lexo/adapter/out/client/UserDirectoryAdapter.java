package app.lexo.adapter.out.client;

import app.lexo.application.port.out.LoadUserContactPort;
import app.lexo.application.port.out.UserContact;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserDirectoryAdapter implements UserExistsPort, LoadUserContactPort {

    private static final Logger log = LoggerFactory.getLogger(UserDirectoryAdapter.class);

    private final UserServiceClient client;

    UserDirectoryAdapter(UserServiceClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "auth-service", fallbackMethod = "existsFallback")
    public boolean exists(OrganizationId organizationId, UserId userId) {
        return client.userExists(userId.value(), organizationId.value()).exists();
    }

    @SuppressWarnings("unused")
    private boolean existsFallback(OrganizationId organizationId, UserId userId, Throwable cause) {
        log.warn("auth-service unavailable (circuit breaker): {}", cause.toString());
        throw new ServiceUnavailableException(
                "Servi\u00e7o de usu\u00e1rios temporariamente indispon\u00edvel. Tente novamente em instantes.");
    }

    @Override
    @CircuitBreaker(name = "auth-service", fallbackMethod = "contactFallback")
    public Optional<UserContact> byId(OrganizationId organizationId, UserId userId) {
        return Optional.ofNullable(client.findUser(userId.value(), organizationId.value()))
                .map(user -> new UserContact(user.name(), user.email()));
    }

    @SuppressWarnings("unused")
    private Optional<UserContact> contactFallback(OrganizationId organizationId, UserId userId, Throwable cause) {
        log.warn("auth-service unavailable (circuit breaker) looking up user {}: {}", userId.value(),
                cause.toString());
        return Optional.empty();
    }
}
