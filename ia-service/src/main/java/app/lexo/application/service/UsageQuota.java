package app.lexo.application.service;

import app.lexo.application.AiSettings;
import app.lexo.application.port.out.RateLimitPort;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class UsageQuota {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimitPort rateLimitPort;
    private final AiSettings settings;

    UsageQuota(RateLimitPort rateLimitPort, AiSettings settings) {
        this.rateLimitPort = rateLimitPort;
        this.settings = settings;
    }

    void spend(OrganizationId organizationId) {
        if (!rateLimitPort.register(organizationId, settings.requestsPerMinute(), WINDOW)) {
            throw new RateLimitExceededException();
        }
    }
}
