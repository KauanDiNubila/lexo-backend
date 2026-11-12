package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserId;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> byEmail(EmailAddress email);

    Optional<User> byId(UserId id);

    Optional<User> inOrganization(OrganizationId organizationId, UserId id);
}
