package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;

import java.time.Duration;

public interface RateLimitPort {

    boolean register(OrganizationId organizationId, int max, Duration window);
}
