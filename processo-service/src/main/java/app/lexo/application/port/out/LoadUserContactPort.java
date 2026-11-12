package app.lexo.application.port.out;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

import java.util.Optional;

public interface LoadUserContactPort {

    Optional<UserContact> byId(OrganizationId organizationId, UserId userId);
}
