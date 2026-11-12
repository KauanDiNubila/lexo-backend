package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

public interface UserExistsPort {

    boolean emailTaken(EmailAddress email);

    boolean existsInOrganization(OrganizationId organizationId, UserId id);
}
