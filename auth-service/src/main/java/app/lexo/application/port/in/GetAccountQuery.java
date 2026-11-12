package app.lexo.application.port.in;

import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public interface GetAccountQuery {

    AccountResponse get(OrganizationId organizationId, UserId userId);
}
