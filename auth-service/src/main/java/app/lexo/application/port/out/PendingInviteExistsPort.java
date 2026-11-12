package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;

import java.time.Instant;

public interface PendingInviteExistsPort {

    boolean exists(EmailAddress email, OrganizationId organizationId, Instant now);
}
